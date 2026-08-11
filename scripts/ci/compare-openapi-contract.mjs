import { readFileSync } from "node:fs";
import { resolve } from "node:path";
import process from "node:process";
import ts from "typescript";

const [expectedArgument, generatedArgument] = process.argv.slice(2);

if (!expectedArgument || !generatedArgument) {
  console.error("Usage: node scripts/ci/compare-openapi-contract.mjs <expected.d.ts> <generated.d.ts>");
  process.exit(2);
}

const printer = ts.createPrinter({ newLine: ts.NewLineKind.LineFeed, removeComments: true });

function sortNodes(nodes, sourceFile) {
  return [...nodes]
    .map((node) => ({
      node,
      key: printer.printNode(ts.EmitHint.Unspecified, node, sourceFile),
    }))
    .sort((left, right) => left.key.localeCompare(right.key, "en"))
    .map(({ node }) => node);
}

function canonicalize(fileArgument) {
  const filePath = resolve(fileArgument);
  const sourceFile = ts.createSourceFile(
    filePath,
    readFileSync(filePath, "utf8"),
    ts.ScriptTarget.Latest,
    true,
    ts.ScriptKind.TS,
  );

  if (sourceFile.parseDiagnostics.length > 0) {
    const host = {
      getCanonicalFileName: (name) => name,
      getCurrentDirectory: () => process.cwd(),
      getNewLine: () => "\n",
    };
    throw new Error(ts.formatDiagnosticsWithColorAndContext(sourceFile.parseDiagnostics, host));
  }

  const transformation = ts.transform(sourceFile, [
    (context) => {
      const visit = (node) => {
        const visited = ts.visitEachChild(node, visit, context);

        if (ts.isInterfaceDeclaration(visited)) {
          return context.factory.updateInterfaceDeclaration(
            visited,
            visited.modifiers,
            visited.name,
            visited.typeParameters,
            visited.heritageClauses,
            sortNodes(visited.members, sourceFile),
          );
        }

        if (ts.isTypeLiteralNode(visited)) {
          return context.factory.updateTypeLiteralNode(
            visited,
            sortNodes(visited.members, sourceFile),
          );
        }

        if (ts.isUnionTypeNode(visited)) {
          return context.factory.createUnionTypeNode(sortNodes(visited.types, sourceFile));
        }

        if (ts.isIntersectionTypeNode(visited)) {
          return context.factory.createIntersectionTypeNode(sortNodes(visited.types, sourceFile));
        }

        return visited;
      };

      return (root) => ts.visitNode(root, visit);
    },
  ]);

  try {
    return printer.printFile(transformation.transformed[0]).replaceAll("\r\n", "\n").trim();
  } finally {
    transformation.dispose();
  }
}

function firstDifference(expected, generated) {
  const expectedLines = expected.split("\n");
  const generatedLines = generated.split("\n");
  const lineCount = Math.max(expectedLines.length, generatedLines.length);

  for (let index = 0; index < lineCount; index += 1) {
    if (expectedLines[index] !== generatedLines[index]) {
      const start = Math.max(0, index - 2);
      const end = Math.min(lineCount, index + 3);
      return {
        line: index + 1,
        expected: expectedLines.slice(start, end).join("\n"),
        generated: generatedLines.slice(start, end).join("\n"),
      };
    }
  }

  return undefined;
}

const expected = canonicalize(expectedArgument);
const generated = canonicalize(generatedArgument);

if (expected !== generated) {
  const difference = firstDifference(expected, generated);
  console.error("The committed TypeScript API contract differs from the live backend OpenAPI document.");
  if (difference) {
    console.error(`First canonical difference at line ${difference.line}.`);
    console.error(`\nCommitted:\n${difference.expected}`);
    console.error(`\nGenerated:\n${difference.generated}`);
  }
  process.exit(1);
}

console.log("OpenAPI contract matches the committed TypeScript schema (member ordering ignored).");
