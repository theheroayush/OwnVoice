# web/tools — offline helpers

Two helpers exist for environments **without network access to
`binaries.prisma.sh`** (Prisma downloads its query/schema engines there, so
`npx prisma generate` fails in air-gapped sandboxes).

They are development aids, not part of the app runtime.

## 1. `prisma-type-shim.py` — schema-accurate `.d.ts` for `tsc`

Generates TypeScript declarations for `@prisma/client` straight from
`schema.prisma`, so `npx tsc --noEmit` and `next build` can type-check real
field names without engines:

```bash
python3 web/tools/prisma-type-shim.py web/prisma/schema.prisma web/node_modules/.prisma/client
python3 web/tools/prisma-type-shim.py ownvoice-web-landing-page/prisma/schema.prisma \
        ownvoice-web-landing-page/node_modules/.prisma/client
cd web && npx tsc --noEmit
```

Relation fields are nullable only when the schema marks them optional (`?`),
matching what Prisma emits.

## 2. `prisma-runtime-stub.js` — last-resort `@prisma/client` runtime

`next build` imports `@prisma/client` at build time. Without a generated client
that import throws `@prisma/client did not initialize yet`. Copy the stub to
make the build (not the queries!) work:

```bash
cp web/tools/prisma-runtime-stub.js web/node_modules/.prisma/client/index.js
cp web/tools/prisma-runtime-stub.js web/node_modules/.prisma/client/default.js
```

It is a permissive `Proxy` that returns empty data for every model call — it
**never** touches a database, so `web/tests/api.test.mjs` intentionally skips
its integration tests while it is installed.

## Preferred path when a network *is* available

```bash
cd web && npx prisma generate && npx prisma db push && npm test
```

This produces the real client and a real SQLite database, and removes the need
for both helpers above.
