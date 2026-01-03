class Kmpli < Formula
  desc "CLI tool for generating Kotlin Multiplatform projects"
  homepage "https://github.com/chornge/kmpli"
  version "1.2.10"
  license "Apache-2.0"

  on_macos do
    if Hardware::CPU.arm?
      url "https://github.com/chornge/kmpli/releases/download/v1.2.10/kmpli-macos-arm64"
      sha256 "12a7389f319e0a8bbeb3aa594e4468e067cfaacc5e96577bf499342bbf8d8f4e"
    else
      url "https://github.com/chornge/kmpli/releases/download/v1.2.10/kmpli-macos-x64"
      sha256 "a3d227a33cc70d879bf1b4d461f8f30eb10f45128c29f7c20b0a9d72aa678852"
    end
  end

  on_linux do
    url "https://github.com/chornge/kmpli/releases/download/v1.2.10/kmpli-linux-x64"
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
