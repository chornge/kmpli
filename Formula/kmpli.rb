class Kmpli < Formula
  desc "Create Kotlin Multiplatform projects from the terminal, no IDE or JVM"
  homepage "https://github.com/chornge/kmpli"
  version "1.7.2"
  license "Apache-2.0"

  on_macos do
    if Hardware::CPU.arm?
      url "https://github.com/chornge/kmpli/releases/download/v1.7.2/kmpli-macos-arm64"
      sha256 "c2464b489da613093073e9cebeac59b68128622a7d2e1c902aebf8f00520c296"
    else
      url "https://github.com/chornge/kmpli/releases/download/v1.7.2/kmpli-macos-x64"
      sha256 "a65899f0dce518ea74dd2e2ff80025bf8400fa58a34fd90b2fb939d27cd103e5"
    end
  end

  on_linux do
    if Hardware::CPU.arm?
      url "https://github.com/chornge/kmpli/releases/download/v1.7.2/kmpli-linux-arm64"
      sha256 "b8da90bb02daa980da13b7d36d0acf7ce3a15662e7f934b19beb5c6bc95fedb8"
    else
      url "https://github.com/chornge/kmpli/releases/download/v1.7.2/kmpli-linux-x64"
      sha256 "6de29d867a3bf9d46ffa9f006a83f1597f3e59368b588289caff4de9db08d544"
    end
  end

  def install
    binary_name = stable.url.split("/").last
    bin.install binary_name => "kmpli"
  end

  test do
    assert_match "Usage:", shell_output("#{bin}/kmpli --help")
  end
end
