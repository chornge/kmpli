class Kmpli < Formula
  desc "CLI tool for generating Kotlin Multiplatform projects"
  homepage "https://github.com/chornge/kmpli"
  version "1.4.0"
  license "Apache-2.0"

  on_macos do
    if Hardware::CPU.arm?
      url "https://github.com/chornge/kmpli/releases/download/v1.4.0/kmpli-macos-arm64"
      sha256 "66853f908b3c266d8a46162180ea3cb1db948e956b5e0c83f8484e6fe77ea101"
    else
      url "https://github.com/chornge/kmpli/releases/download/v1.4.0/kmpli-macos-x64"
      sha256 "b443244b9b4c5a75f4c35a54b2fb2a5fa2734dbd6c1d89e21f5abe7ad9d384cf"
    end
  end

  on_linux do
    url "https://github.com/chornge/kmpli/releases/download/v1.4.0/kmpli-linux-x64"
    sha256 "ed784408d06014dbba5296a3b46dfb2798729aaecd82e0816f7374a8418731de"
  end

  def install
    binary_name = stable.url.split("/").last
    bin.install binary_name => "kmpli"
  end

  test do
    assert_match "Usage:", shell_output("#{bin}/kmpli --help")
  end
end
