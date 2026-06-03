export const environment = {
  production: true,
  // GitHub Pages deployment: Angular is hosted on GitHub CDN,
  // but the backend runs locally via Docker on the showcase computer.
  // The viewer must open https://localhost once and trust the self-signed cert.
  apiUrl: 'https://localhost/api'
};
