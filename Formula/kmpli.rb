class Kmpli < Formula
  desc "CLI tool for generating Kotlin Multiplatform projects"
  homepage "https://github.com/chornge/kmpli"
  version "1.6.1"
  license "Apache-2.0"

  on_macos do
    if Hardware::CPU.arm?
      url "https://github.com/chornge/kmpli/releases/download/v1.6.1/kmpli-macos-arm64"
      sha256 "ffd57d31ded1e58a77e4ad413ec41ea676e791507891e25116b7e7291763e6be"
    else
      url "https://github.com/chornge/kmpli/releases/download/v1.6.1/kmpli-macos-x64"
      sha256 "26c9e8945f3342c2fbf979432f0556df365464213d6af406165e78fd6e7e693b"
    end
  end

  on_linux do
    if Hardware::CPU.arm?
      url "https://github.com/chornge/kmpli/releases/download/v1.6.1/kmpli-linux-arm64"
      sha256 "ca6a8d3de613aeb60849ea70f089e656c81a93c57cce24377dbe7bd05f68f6b9"
    else
      url "https://github.com/chornge/kmpli/releases/download/v1.6.1/kmpli-linux-x64"
      sha256 "24d8e9611b9134aea83f26e0fdf17e0662554ab45b22a48dc3dd8356ce2a0232"
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
