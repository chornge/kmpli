class Kmpli < Formula
  desc "CLI tool for generating Kotlin Multiplatform projects"
  homepage "https://github.com/chornge/kmpli"
  version "1.2.9"
  license "Apache-2.0"

  on_macos do
    if Hardware::CPU.arm?
      url "https://github.com/chornge/kmpli/releases/download/v1.2.9/kmpli-macos-arm64"
      sha256 "3dc769b062fdcf2dd17254ab6140b919b89efbf4ca2d6afd5c2978dbe69a6d45"
    else
      url "https://github.com/chornge/kmpli/releases/download/v1.2.9/kmpli-macos-x64"
      sha256 "387a4b52a0b5f400ebfceb62d76f7424a5c499ca61b2014557c99733b2d8dab4"
    end
  end

  on_linux do
    url "https://github.com/chornge/kmpli/releases/download/v1.2.9/kmpli-linux-x64"
    sha256 "ac04532899cf27b44bb5a4d4c609b0d459f33928ed274686d02994b6d3b9dc9d"
  end

  def install
    binary_name = stable.url.split("/").last
    bin.install binary_name => "kmpli"
  end

  test do
    assert_match "Usage:", shell_output("#{bin}/kmpli --help")
  end
end
