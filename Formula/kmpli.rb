class Kmpli < Formula
  desc "Create Kotlin Multiplatform projects from the terminal, no IDE or JVM"
  homepage "https://github.com/chornge/kmpli"
  version "1.7.1"
  license "Apache-2.0"

  on_macos do
    if Hardware::CPU.arm?
      url "https://github.com/chornge/kmpli/releases/download/v1.7.1/kmpli-macos-arm64"
      sha256 "14f71f8f51f924927e2ac188fdb97005463976b53cc58ef00040eaf770581702"
    else
      url "https://github.com/chornge/kmpli/releases/download/v1.7.1/kmpli-macos-x64"
      sha256 "b05b9878a59f46f1ff47e8c8b6bc2f68d981d8dc4214e8510451570de16ee3be"
    end
  end

  on_linux do
    if Hardware::CPU.arm?
      url "https://github.com/chornge/kmpli/releases/download/v1.7.1/kmpli-linux-arm64"
      sha256 "3a22338ca50556f5c0f81fb78dc6aec3260c08053f2e6870e7648a92afb64ff6"
    else
      url "https://github.com/chornge/kmpli/releases/download/v1.7.1/kmpli-linux-x64"
      sha256 "38fc37f29eef8078a5ea7a75def6cde5ff2464f848ed8a271d0eec63a90d232e"
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
