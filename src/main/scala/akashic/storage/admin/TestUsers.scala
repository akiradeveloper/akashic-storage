package akashic.storage.admin

object TestUsers {
  val hoge = User(
    id = "123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef0",
    accessKey = "BCDEFGHIJKLMNOPQRSTA",
    secretKey = "bcdefghijklmnopqrstuvwxyzabcdefghijklmna",
    name = "hoge",
    email = "hoge@hoge.com",
    displayName = "hoge"
  )

  val s3testsMain = User(
    id = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef",
    accessKey = "ABCDEFGHIJKLMNOPQRST",
    secretKey = "abcdefghijklmnopqrstuvwxyzabcdefghijklmn",
    name = "s3tests-main",
    email = "main@s3tests.com",
    displayName = "s3tests-main"
  )

  val s3testsAlt = User(
    id = "56789abcdef0123456789abcdef0123456789abcdef0123456789abcdef01234",
    accessKey = "NOPQRSTUVWXYZABCDEFG",
    secretKey = "nopqrstuvwxyzabcdefghijklmnabcdefghijklm",
    name = "s3tests-alt",
    email = "alt@s3tests.com",
    displayName = "s3tests-alt"
  )
}
