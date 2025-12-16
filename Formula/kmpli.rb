class Kmpli < Formula
  desc "CLI tool for generating Kotlin Multiplatform projects"
  homepage "https://github.com/chornge/kmpli"
  version "1.2.6"
  license "Apache-2.0"

  on_macos do
    if Hardware::CPU.arm?
      url "https://github.com/chornge/kmpli/releases/download/v#{version}/kmpli-macos-arm64"
      sha256 "9265df6300d478e899cdcc3a05863cada037e73e1b25e8df7c8ff12e1d391f6b"
    else
      url "https://github.com/chornge/kmpli/releases/download/v#{version}/kmpli-macos-x64"
      sha256 "79dc6c3c317df11dc9f5c6a3cba7eb62788719e65f6f66d40c322dc7502493ee"
    end
  end

  on_linux do
    url "https://github.com/chornge/kmpli/releases/download/v#{version}/kmpli-linux-x64"
    sha256 "6c65eae01e237b9ad73407253c11faf1c9413e490302df539601e4f174c65915"
  end

  def install
    binary_name = stable.url.split("/").last
    bin.install binary_name => "kmpli"
  end

  test do
    assert_match "Usage:", shell_output("#{bin}/kmpli --help")
  end
end
