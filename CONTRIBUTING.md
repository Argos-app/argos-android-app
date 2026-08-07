# Guia de Contribuição

Guia de configuração e fluxo de trabalho para novos desenvolvedores no projeto **Argos Android App**.

---

## Índice

- [Pré-requisitos](#pré-requisitos)
- [Configuração do ambiente local](#configuração-do-ambiente-local)
- [Rodando a aplicação](#rodando-a-aplicação)
- [Rodando os testes](#rodando-os-testes)
- [Conventional Commits](#conventional-commits)
- [Fluxo de branches](#fluxo-de-branches)
- [Abrindo um Pull Request](#abrindo-um-pull-request)

---

## Pré-requisitos

Antes de começar, certifique-se de ter instalado:

- [Android Studio](https://developer.android.com/studio) (recomendado)
- [JDK 17](https://adoptium.net) ou superior (o projeto compila com `JavaVersion.VERSION_11`)
- [Node.js](https://nodejs.org) (necessário para os hooks de commit via Husky)
- [Git](https://git-scm.com)
- Um projeto [Firebase](https://firebase.google.com) configurado para Android

---

## Configuração do ambiente local

### 1. Clone o repositório

```bash
git clone https://github.com/Argos-app/argos-android-app.git
cd argos-android-app
```

### 2. Instale as dependências Node

O repositório usa Node para os hooks de commit via Husky + commitlint.

```bash
npm install
```

> Este passo é obrigatório. Sem ele, o commitlint não vai funcionar e seus commits serão bloqueados.

### 3. Configure o Firebase

O plugin `google-services` exige o arquivo `app/google-services.json`. Coloque o arquivo do seu projeto Firebase na raiz do módulo `app/`.

> [!WARNING]
> Este arquivo está ignorado pelo Git (`gitignore`) e **não deve** ser versionado. Em CI, ele é injetado via secret `GOOGLE_SERVICES_JSON`.

### 4. Abra o projeto

1. Abra o **Android Studio**
2. Selecione **Open** e escolha a pasta raiz do projeto
3. Aguarde a sincronização do Gradle terminar

---

## Rodando a aplicação

Com o projeto sincronizado, clique no botão **Run** (▶) na toolbar após selecionar o módulo `app` e um dispositivo/emulador.

Alternativamente, pelo terminal:

```bash
# Gera o APK de debug
./gradlew assembleDebug
```

O APK será gerado em `app/build/outputs/apk/debug/`.

---

## Rodando os testes

O projeto possui testes unitários com JUnit, MockK e coroutines-test. Para executá-los:

```bash
./gradlew testDebugUnitTest
```

Os relatórios de teste ficam em `app/build/reports/`.

---

## Conventional Commits

Este repositório segue o padrão [Conventional Commits](https://www.conventionalcommits.org). O formato é obrigatório e validado automaticamente pelo commitlint via Husky ao fazer commit.

### Formato

```
<tipo>(<escopo>): <descrição curta>
```

### Tipos aceitos

| Tipo | Quando usar |
|---|---|
| `feat` | Nova funcionalidade |
| `fix` | Correção de bug |
| `refactor` | Refatoração sem mudança de comportamento |
| `chore` | Tarefas de manutenção, dependências, configs |
| `docs` | Documentação |
| `test` | Testes |
| `perf` | Melhoria de performance |
| `ci` | Mudanças na pipeline de CI/CD |

### Exemplos

```bash
git commit -m "feat(auth): adiciona autenticação com Google"
git commit -m "fix(auth): corrige validação do token"
git commit -m "chore(deps): atualiza compose bom"
git commit -m "docs: atualiza guia de contribuição"
```

> [!IMPORTANT]
> Commits fora desse padrão serão rejeitados automaticamente.

---

## Fluxo de branches

```
feature/minha-feature → develop → main
```

| Branch | Propósito |
|---|---|
| `main` | Código em produção |
| `develop` | Integração antes de ir para produção |
| `feature/*` | Desenvolvimento de novas funcionalidades |
| `fix/*` | Correções de bugs |
| `chore/*` | Manutenção e configurações |

### Criando uma branch

```bash
git checkout develop
git pull
git checkout -b feature/numero-da-feature
```

---

## Abrindo um Pull Request

1. Faça push da sua branch para o repositório remoto
2. Abra um Pull Request apontando para `develop`
3. Preencha o template de PR completamente
4. Aguarde a pipeline de CI passar e ao menos uma aprovação antes do merge
5. Use **Squash Merge**; o título do PR vira o commit de merge e deve seguir o padrão Conventional Commits