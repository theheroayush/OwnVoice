"""Generate a TypeScript declaration shim for the Prisma client from prisma/schema.prisma.

Used only in sandboxes where `prisma generate` cannot download engines (no network).
Writes to node_modules/.prisma/client/*.d.ts so `tsc` can type-check app code
against the real schema field names.
"""
import re
import sys
from pathlib import Path

SCHEMA = Path(sys.argv[1] if len(sys.argv) > 1 else "web/prisma/schema.prisma")
OUT_DIR = Path(sys.argv[2] if len(sys.argv) > 2 else "web/node_modules/.prisma/client")

text = SCHEMA.read_text()
text = re.sub(r"//.*", "", text)

TYPE_MAP = {
    "String": "string",
    "Int": "number",
    "Float": "number",
    "Boolean": "boolean",
    "DateTime": "Date",
    "Json": "any",
    "Bytes": "Uint8Array",
    "BigInt": "bigint",
    "Decimal": "any",
}

models = {}
for m in re.finditer(r"model\s+(\w+)\s*\{(.*?)\n\}", text, re.S):
    name, body = m.group(1), m.group(2)
    fields = {}
    relations = []
    for line in body.splitlines():
        line = line.strip()
        if not line or line.startswith("@") or line.startswith("@@") or line.startswith("//"):
            continue
        parts = line.split()
        if len(parts) < 2:
            continue
        fname, ftype = parts[0], parts[1]
        optional = ftype.endswith("?")
        ftype = ftype.rstrip("?")
        array = ftype.endswith("[]")
        ftype = ftype.rstrip("[]")
        if ftype in models or (ftype[0].isupper() and ftype not in TYPE_MAP):
            relations.append((fname, ftype, array, optional))
        else:
            ts = TYPE_MAP.get(ftype, "any")
            fields[fname] = (f"{ts} | null" if optional else ts, False, True)
    models[name] = {"fields": fields, "relations": relations}
    # scalar defaults: check for @default / @updatedAt on scalar lines already handled

lines = []
lines.append("// AUTO-GENERATED type shim (schema-mirroring) — do not edit.")
lines.append("/* eslint-disable */")
lines.append("declare namespace PrismaShim {")
for name, spec in models.items():
    lines.append(f"  export interface {name} {{")
    for fname, (ts, optional, _) in spec["fields"].items():
        lines.append(f"    {fname}{'?' if optional else ''}: {ts};")
    for fname, ftype, array, optional in spec["relations"]:
        if array:
            rel = f"{ftype}[]"
        elif optional:
            rel = f"{ftype} | null"
        else:
            rel = ftype
        lines.append(f"    {fname}: {rel};")
        lines.append(f"    _{fname}?: any;")
    lines.append("  }")
lines.append("}")
lines.append("import PrismaShim = PrismaShim;")
lines.append("export import Prisma = PrismaShim;")

model_names = list(models)
lines.append("type ModelName = " + " | ".join(f'"{m}"' for m in model_names) + ";")
lines.append("type ScalarWhere<M> = { [K in keyof M]?: any } & { AND?: any; OR?: any; NOT?: any; equals?: any; in?: any; notIn?: any };")
lines.append("""
interface Delegate<M extends { id?: any }> {
  findMany(args?: { where?: ScalarWhere<M>; orderBy?: { [K in keyof M]?: any } | { [K in keyof M]?: any }[]; select?: { [K in keyof M]?: any } & { _count?: any }; include?: any; take?: number; skip?: number; cursor?: any; distinct?: any }): Promise<M[]>;
  findFirst(args?: { where?: ScalarWhere<M>; orderBy?: { [K in keyof M]?: any } | { [K in keyof M]?: any }[]; select?: { [K in keyof M]?: any } & { _count?: any }; include?: any }): Promise<M | null>;
  findFirstOrThrow(args?: { where?: ScalarWhere<M> }): Promise<M>;
  findUnique(args: { where: any; select?: { [K in keyof M]?: any } & { _count?: any }; include?: any }): Promise<M | null>;
  findUniqueOrThrow(args: { where: any }): Promise<M>;
  create(args: { data: { [K in keyof M]?: any }; select?: { [K in keyof M]?: any } & { _count?: any }; include?: any }): Promise<M>;
  createMany(args?: { data: { [K in keyof M]?: any }; skipDuplicates?: boolean }): Promise<{ count: number }>;
  update(args: { where: any; data: { [K in keyof M]?: any }; select?: { [K in keyof M]?: any } & { _count?: any }; include?: any }): Promise<M>;
  updateMany(args?: { where?: ScalarWhere<M>; data: { [K in keyof M]?: any } & Record<string, any> }): Promise<{ count: number }>;
  upsert(args: { where: any; create: { [K in keyof M]?: any }; update: { [K in keyof M]?: any }; select?: { [K in keyof M]?: any } & { _count?: any }; include?: any }): Promise<M>;
  delete(args: { where: any; select?: { [K in keyof M]?: any } & { _count?: any }; include?: any }): Promise<M>;
  deleteMany(args?: { where?: ScalarWhere<M> }): Promise<{ count: number }>;
  count(args?: { where?: ScalarWhere<M>; orderBy?: { [K in keyof M]?: any } | { [K in keyof M]?: any }[]; skip?: number; take?: number; select?: any }): Promise<number>;
  aggregate(args?: any): Promise<any>;
  groupBy(args?: any): Promise<any[]>;
}

export declare class PrismaClient {
  constructor(options?: any);
  $connect(): Promise<void>;
  $disconnect(): Promise<void>;
  $queryRaw(...args: any[]): Promise<any>;
  $executeRaw(...args: any[]): Promise<any>;
  $transaction(...args: any[]): Promise<any>;
""")
for name in model_names:
    lines.append(f"  {name[0].lower() + name[1:]}: Delegate<PrismaShim.{name}>;")
lines.append("  [key: string]: any;")
lines.append("}")
lines.append("export default PrismaClient;")
lines.append("export declare namespace Prisma {")
lines.append("  export type PrismaClient = any;")
lines.append("  export const PrismaClientKnownRequestError: any;")
lines.append("  export type TransactionClient = any;")
lines.append("}")
out = "\n".join(lines) + "\n"
OUT_DIR.mkdir(parents=True, exist_ok=True)
for fname in ["index.d.ts", "default.d.ts", "edge.d.ts", "wasm.d.ts"]:
    (OUT_DIR / fname).write_text(out)
print(f"wrote shim covering {len(model_names)} models: {', '.join(model_names)}")
