// Sandbox-only fake Prisma runtime (empty database) for SSR smoke testing.
// It answers every query with empty data, so integration tests MUST skip
// instead of asserting against it — see the __OWNVOICE_PRISMA_STUB__ marker.
globalThis.__OWNVOICE_PRISMA_STUB__ = true;
class PrismaClient {
  constructor() {
    const delegate = new Proxy({}, {
      get: (_t, method) => {
        return async () => {
          if (method === "count") return 0;
          if (method === "aggregate") return { _count: 0, _sum: {}, _avg: {} };
          if (method === "groupBy") return [];
          if (method === "createMany") return { count: 0 };
          if (method === "updateMany" || method === "deleteMany") return { count: 0 };
          if (method === "findMany") return [];
          return null;
        };
      },
    });
    return new Proxy(this, {
      get: (t, prop) => {
        if (prop in t) return t[prop];
        if (typeof prop === "string" && prop.startsWith("$")) return async () => null;
        return delegate;
      },
    });
  }
  $transaction(...args) { return Promise.resolve(args.flatMap((a) => (a ? [a] : []))); }
  $connect() { return Promise.resolve(); }
  $disconnect() { return Promise.resolve(); }
  $extends() { return this; }
}
const Prisma = { defineExtension: (e) => e, getExtensionContext: (t) => t, prismaVersion: { client: "6.19.3", engine: "sandbox" } };
module.exports = { PrismaClient, Prisma, default: { Prisma } };
