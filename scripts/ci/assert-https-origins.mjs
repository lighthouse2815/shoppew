const names = process.argv.slice(2);

if (names.length === 0) {
  throw new Error("Pass at least one environment-variable name to validate");
}

for (const name of names) {
  const value = process.env[name]?.trim();
  if (!value) {
    throw new Error(`${name} is required when building a production web image`);
  }

  const url = new URL(value);
  if (
    url.protocol !== "https:" ||
    url.username ||
    url.password ||
    url.search ||
    url.hash ||
    url.pathname !== "/"
  ) {
    throw new Error(`${name} must be an HTTPS origin without credentials, path, query, or hash`);
  }
}
