import * as acorn from 'acorn';

const code = `const { capture } = require('@/features/payment/panel/capture')`;
const ast = acorn.parse(code, { ecmaVersion: 2024, sourceType: 'module' });

function findCallExpression(node) {
  if (!node || typeof node !== 'object') return;
  
  if (node.type === 'CallExpression' && node.callee && node.callee.name === 'require') {
    console.log('FOUND require() CallExpression');
    if (node.arguments && node.arguments[0]) {
      console.log(`  Argument value: "${node.arguments[0].value}"`);
    }
  }
  
  for (const key in node) {
    if (key !== 'loc' && key !== 'start' && key !== 'end') {
      if (Array.isArray(node[key])) {
        node[key].forEach(findCallExpression);
      } else if (typeof node[key] === 'object') {
        findCallExpression(node[key]);
      }
    }
  }
}

findCallExpression(ast);
