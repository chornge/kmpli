class Kmpli < Formula
  desc "CLI tool for generating Kotlin Multiplatform projects"
  homepage "https://github.com/chornge/kmpli"
  version "1.2.7"
  license "Apache-2.0"

  on_macos do
    if Hardware::CPU.arm?
      url "https://github.com/chornge/kmpli/releases/download/v#{version}/kmpli-macos-arm64"
      sha256 "9b7befb7c27380f302648aa5f0d690f1f970adfbac5f9fd8d4f7856bb600cd82"
    else
      url "https://github.com/chornge/kmpli/releases/download/v#{version}/kmpli-macos-x64"
      sha256 "6c0038706ee27f5df14725c82218b5b7715de53bc6f934c52b934d08b6356ac9"
    end
  end

  on_linux do
    url "https://github.com/chornge/kmpli/releases/download/v#{version}/kmpli-linux-x64"
    sha256 "b13fc62e2124343dab2092ab2f306b47013c18bea5d546d213fa03720a0e64fc"
  end

  def install
    binary_name = stable.url.split("/").last
    bin.install binary_name => "kmpli"
  end

  test do
    assert_match "Usage:", shell_output("#{bin}/kmpli --help")
  end
end
