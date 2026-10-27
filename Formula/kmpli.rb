class Kmpli < Formula
  desc "Create Kotlin Multiplatform projects from the terminal, no IDE or JVM"
  homepage "https://github.com/chornge/kmpli"
  version "1.7.3"
  license "Apache-2.0"

  on_macos do
    if Hardware::CPU.arm?
      url "https://github.com/chornge/kmpli/releases/download/v1.7.3/kmpli-macos-arm64"
      sha256 "a118f68e4014879dd5062d83105ce29b5f3fca1ea471aa3161077175097debca"
    else
      url "https://github.com/chornge/kmpli/releases/download/v1.7.3/kmpli-macos-x64"
      sha256 "40baffd141e618f62424f378cfb151e25b14d7e385763062ef5433225b3d3eda"
    end
  end

  on_linux do
    if Hardware::CPU.arm?
      url "https://github.com/chornge/kmpli/releases/download/v1.7.3/kmpli-linux-arm64"
      sha256 "ccbb9e45502d5e72830ae8fff937b4ddaf625c18d667ece2a765f155d9eaa837"
    else
      url "https://github.com/chornge/kmpli/releases/download/v1.7.3/kmpli-linux-x64"
      sha256 "06dc6317853df3bfcde684db4561c2d8ca6c996f88b3744cb59499346ab93b6a"
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
