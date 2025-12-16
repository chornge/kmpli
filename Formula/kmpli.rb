class Kmpli < Formula
  desc "CLI tool for generating Kotlin Multiplatform projects"
  homepage "https://github.com/chornge/kmpli"
  version "1.2.4"
  license "Apache-2.0"

  on_macos do
    if Hardware::CPU.arm?
      url "https://github.com/chornge/kmpli/releases/download/v#{version}/kmpli-macos-arm64"
      sha256 "" # Will be updated by release workflow
    else
      url "https://github.com/chornge/kmpli/releases/download/v#{version}/kmpli-macos-x64"
      sha256 "" # Will be updated by release workflow
    end
  end

  on_linux do
    if Hardware::CPU.arm?
      url "https://github.com/chornge/kmpli/releases/download/v#{version}/kmpli-linux-arm64"
      sha256 "" # Will be updated by release workflow
    else
      url "https://github.com/chornge/kmpli/releases/download/v#{version}/kmpli-linux-x64"
      sha256 "" # Will be updated by release workflow
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
