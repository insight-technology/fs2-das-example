package pure.kms.tagless

import software.amazon.awssdk.services.kms.model.{DecryptRequest, DecryptResponse}


trait KmsAsyncClientOp[F[_]] {
  def close: F[Unit]
  def decrypt(decryptRequest: DecryptRequest): F[DecryptResponse]

}
