class Kmpli < Formula
  desc "Create Kotlin Multiplatform projects from the terminal, no IDE or JVM"
  homepage "https://github.com/chornge/kmpli"
  version "1.7.4"
  license "Apache-2.0"

  on_macos do
    if Hardware::CPU.arm?
      url "https://github.com/chornge/kmpli/releases/download/v1.7.4/kmpli-macos-arm64"
      sha256 "5206f435f1b810108eb2689a3eea9a0da962d39e1588e0f3b829bdf6f73d5eda"
    else
      url "https://github.com/chornge/kmpli/releases/download/v1.7.4/kmpli-macos-x64"
      sha256 "04999d035f483ef886282b696df882a995025441cc8007cbd28da170d746f2fe"
    end
  end

  on_linux do
    if Hardware::CPU.arm?
      url "https://github.com/chornge/kmpli/releases/download/v1.7.4/kmpli-linux-arm64"
      sha256 "ed74dfd848ece6d7457e8e1d405391a98bf623955b1e57875b48adeff4e017e8"
    else
      url "https://github.com/chornge/kmpli/releases/download/v1.7.4/kmpli-linux-x64"
      sha256 "472f13931098724b074e9050acccef820d76545136d24b82208c0bd608a86ff1"
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
