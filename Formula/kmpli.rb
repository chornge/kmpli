class Kmpli < Formula
  desc "CLI tool for generating Kotlin Multiplatform projects"
  homepage "https://github.com/chornge/kmpli"
  version "1.5.0"
  license "Apache-2.0"

  on_macos do
    if Hardware::CPU.arm?
      url "https://github.com/chornge/kmpli/releases/download/v1.5.0/kmpli-macos-arm64"
      sha256 "67d3b5b03b41889fe00f6ce2812bfcad9f601829a9961dcefa7f567613fbe695"
    else
      url "https://github.com/chornge/kmpli/releases/download/v1.5.0/kmpli-macos-x64"
      sha256 "5d0f44297930f643025c14b21b30338ccefaed83b053a9d07e93f1f091be5b6a"
    end
  end

  on_linux do
    if Hardware::CPU.arm?
      url "https://github.com/chornge/kmpli/releases/download/v1.5.0/kmpli-linux-arm64"
      sha256 "d2049382a0cbada3243ffa3541345d40e1987e0a66e6d69e5bc678f93bd9af3d"
    else
      url "https://github.com/chornge/kmpli/releases/download/v1.5.0/kmpli-linux-x64"
      sha256 "cd1d9c21e07ce2e5202de5ceb81f63a6e701e64d630da30755727af9057a6708"
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
