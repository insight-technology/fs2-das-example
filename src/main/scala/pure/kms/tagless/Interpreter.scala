package pure.kms.tagless

import cats.data.Kleisli
import cats.effect.{Async, Resource}
import software.amazon.awssdk.services.kms.model.{DecryptRequest, DecryptResponse}
import software.amazon.awssdk.services.kms.{KmsAsyncClient, KmsAsyncClientBuilder, KmsClient}

import java.util.concurrent.{CompletableFuture, CompletionException}

object Interpreter {
  def apply[M[_]](
      implicit am: Async[M]
  ): Interpreter[M] =
    new Interpreter[M] {
      val asyncM: Async[M] = am
    }

}

trait Interpreter[M[_]] {
  outer =>

  implicit val asyncM: Async[M]

  // to support shifting blocking operations to another pool.

  def primitive[J, A](f: J => A): Kleisli[M, J, A] = Kleisli(a => asyncM.blocking(f(a)))

  def primitive1[J, A](f: => A): M[A] = asyncM.blocking(f)

  def eff[J, A](fut: J => CompletableFuture[A]): Kleisli[M, J, A] = Kleisli { a =>
    asyncM.async_ { cb =>
      fut(a).handle[Unit] { (a, x) =>
        if (a == null)
          x match {
            case t: CompletionException => cb(Left(t.getCause))
            case t                      => cb(Left(t))
          }
        else
          cb(Right(a))
      }
      ()
    }
  }

  def eff1[J, A](fut: => CompletableFuture[A]): M[A] =
    asyncM.async_ { cb =>
      fut.handle[Unit] { (a, x) =>
        if (a == null)
          x match {
            case t: CompletionException => cb(Left(t.getCause))
            case t => cb(Left(t))
          }
        else
          cb(Right(a))
      }
      ()
    }

  def KmsAsyncClientResource(builder: KmsAsyncClientBuilder): Resource[M, KmsAsyncClient] =
    Resource.fromAutoCloseable(asyncM.delay(builder.build()))

  def KmsAsyncClientOpResource(builder: KmsAsyncClientBuilder): Resource[M, KmsAsyncClientOp[M]] =
    KmsAsyncClientResource(builder).map(create)

  def create(client: KmsAsyncClient): KmsAsyncClientOp[M] = new KmsAsyncClientOp[M] {
    override def close: M[Unit] = primitive1(client.close())

    override def decrypt(decryptRequest: DecryptRequest): M[DecryptResponse] = eff1(client.decrypt(decryptRequest))
  }
}