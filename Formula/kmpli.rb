class Kmpli < Formula
  desc "CLI tool for generating Kotlin Multiplatform projects"
  homepage "https://github.com/chornge/kmpli"
  version "1.3.0"
  license "Apache-2.0"

  on_macos do
    if Hardware::CPU.arm?
      url "https://github.com/chornge/kmpli/releases/download/v1.3.0/kmpli-macos-arm64"
      sha256 "236f2a7ee5413fca84d75fe65f1117a50bbd6420f453d477d5bfa5b3de337efd"
    else
      url "https://github.com/chornge/kmpli/releases/download/v1.3.0/kmpli-macos-x64"
      sha256 "e11b14b8b69ae618263bfe4815f9d5bb683566992494594a99fafb712a75d0f3"
    end
  end

  on_linux do
    url "https://github.com/chornge/kmpli/releases/download/v1.3.0/kmpli-linux-x64"
    sha256 "26abbe566938585bba6d3f636321f69ea83b72a57ebeff2b7d8951e9fc69cb29"
  end

  def install
    binary_name = stable.url.split("/").last
    bin.install binary_name => "kmpli"
  end

  test do
    assert_match "Usage:", shell_output("#{bin}/kmpli --help")
  end
end
