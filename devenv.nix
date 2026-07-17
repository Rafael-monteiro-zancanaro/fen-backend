{
  pkgs,
  lib,
  config,
  ...
}:
{
  # https://devenv.sh/languages/
  languages = {
    java = {
      enable = true;
      jdk.package = pkgs.openjdk25;
      gradle.enable = true;
    };
  };

  # https://devenv.sh/services/
  services = {
    postgres = {
      enable = true;
      initialDatabases = [ { name = "fendb"; } ];
    };
  };

  # See full reference at https://devenv.sh/reference/options/
}