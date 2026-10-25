class Kmpli < Formula
  desc "CLI tool for generating Kotlin Multiplatform projects"
  homepage "https://github.com/chornge/kmpli"
  version "1.7.0"
  license "Apache-2.0"

  on_macos do
    if Hardware::CPU.arm?
      url "https://github.com/chornge/kmpli/releases/download/v1.7.0/kmpli-macos-arm64"
      sha256 "5b7dc57c6d268081de62e33e3f9e6e803049da001e36a05f51401515edc8546f"
    else
      url "https://github.com/chornge/kmpli/releases/download/v1.7.0/kmpli-macos-x64"
      sha256 "7f2809b2f9e26d8dea30e70e72b1b2754c10906549cae3d7555ab0fcf992cabd"
    end
  end

  on_linux do
    if Hardware::CPU.arm?
      url "https://github.com/chornge/kmpli/releases/download/v1.7.0/kmpli-linux-arm64"
      sha256 "934d6167eb6a42a63f14e7c3614cbf1db373e27c7e68834ed786c0d97f2687e6"
    else
      url "https://github.com/chornge/kmpli/releases/download/v1.7.0/kmpli-linux-x64"
      sha256 "871dbec94fc9a1a8bd45432dee4bfc809ea1449996e8c00a4527bde511890f66"
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
