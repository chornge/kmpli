class Kmpli < Formula
  desc "CLI tool for generating Kotlin Multiplatform projects"
  homepage "https://github.com/chornge/kmpli"
  version "1.2.11"
  license "Apache-2.0"

  on_macos do
    if Hardware::CPU.arm?
      url "https://github.com/chornge/kmpli/releases/download/v1.2.11/kmpli-macos-arm64"
      sha256 "30de0a3c365ae6a22936aa3a8bd63f42203f6ec229776e511e5180050eedc848"
    else
      url "https://github.com/chornge/kmpli/releases/download/v1.2.11/kmpli-macos-x64"
      sha256 "44b6b5b506726d5eb2822ab3f664ff4bcb9a379af113e145e747f7ead03eb529"
    end
  end

  on_linux do
    url "https://github.com/chornge/kmpli/releases/download/v1.2.11/kmpli-linux-x64"
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
