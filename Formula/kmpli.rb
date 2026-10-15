class Kmpli < Formula
  desc "CLI tool for generating Kotlin Multiplatform projects"
  homepage "https://github.com/chornge/kmpli"
  version "1.6.0"
  license "Apache-2.0"

  on_macos do
    if Hardware::CPU.arm?
      url "https://github.com/chornge/kmpli/releases/download/v1.6.0/kmpli-macos-arm64"
      sha256 "29b950f08656865f5a8c8ae0fc74bcb6b227323541d39bd69e3037e591409adc"
    else
      url "https://github.com/chornge/kmpli/releases/download/v1.6.0/kmpli-macos-x64"
      sha256 "979df37c02ab08c819b123711f0751f54246aa46d3090851a0d2b124afeb4c43"
    end
  end

  on_linux do
    if Hardware::CPU.arm?
      url "https://github.com/chornge/kmpli/releases/download/v1.6.0/kmpli-linux-arm64"
      sha256 "6acfe09d016f87f4df4da062ac4e36f35963a8351fea93cda55b1918c926be9d"
    else
      url "https://github.com/chornge/kmpli/releases/download/v1.6.0/kmpli-linux-x64"
      sha256 "2fd8e446cff1e073c1346ce06ddcd0b8483d33d07d436784e833fe18f2b7963e"
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
