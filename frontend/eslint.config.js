import js from '@eslint/js'
import eslintConfigPrettier from '@vue/eslint-config-prettier/skip-formatting'
import pluginVue from 'eslint-plugin-vue'

const cypressGlobals = {
  Cypress: 'readonly',
  after: 'readonly',
  afterEach: 'readonly',
  assert: 'readonly',
  before: 'readonly',
  beforeEach: 'readonly',
  context: 'readonly',
  cy: 'readonly',
  describe: 'readonly',
  expect: 'readonly',
  it: 'readonly',
  localStorage: 'readonly',
  require: 'readonly',
  sessionStorage: 'readonly',
  setTimeout: 'readonly',
  console: 'readonly',
}

export default [
  {
    ignores: ['dist/**', 'node_modules/**', '**/*.ts'],
  },
  js.configs.recommended,
  ...pluginVue.configs['flat/essential'],
  {
    files: ['cypress/**/*.{js,ts,vue}'],
    languageOptions: {
      globals: cypressGlobals,
    },
  },
  {
    files: ['**/*.vue'],
    languageOptions: {
      parserOptions: {
        // Vue templates are linted here; vue-tsc checks the TypeScript scripts.
        parser: false,
      },
    },
  },
  {
    files: ['src/components/Toast.vue'],
    rules: {
      'vue/multi-word-component-names': ['error', { ignores: ['Toast'] }],
    },
  },
  eslintConfigPrettier,
]
