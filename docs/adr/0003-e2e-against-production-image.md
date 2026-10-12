# End-to-end tests run against the production Docker image

The Playwright suite in `e2e/` starts the same Docker image we publish (SPA built into the backend, served same-origin), on a throwaway Postgres, rather than `ng serve` + `spring-boot:run` from `dev.sh`. The dev setup proxies `/api` through the Angular dev server and skips the SPA fallback, static serving and image packaging, which are exactly the seams unit and integration tests can't reach; we accept slower local iteration (an image build per run unless `E2E_NO_BUILD=1`) to test what users actually run.
