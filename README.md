<div align="center">

# Argos - argos.android.app

Aplicativo móvel nativo Android do **Argos**.

![Android](https://img.shields.io/badge/Android-36-brightgreen?logo=android)
![Kotlin](https://img.shields.io/badge/Kotlin-2.0.21-purple?logo=kotlin)
![Compose](https://img.shields.io/badge/UI-Compose%20(Material%203)-darkblue?logo=jetpackcompose)
![AGP](https://img.shields.io/badge/AGP-9.0.1-orange?logo=gradle)
![Gradle](https://img.shields.io/badge/Gradle-9.1.0-lightblue?logo=gradle)
[![CI](https://github.com/Argos-app/argos-android-app/actions/workflows/ci.yml/badge.svg)](https://github.com/Argos-app/argos-android-app/actions/workflows/ci.yml)

</div>

---

## Pré-requisitos

- [Android Studio](https://developer.android.com/studio) (recomendado)
- **JDK 17** ou superior (projeto compila com `JavaVersion.VERSION_11`)

## Como executar

1. **Clone o repositório**

   ```bash
   git clone https://github.com/Argos-app/argos-android-app.git
   cd argos-android-app
   ```

2. **Configure o Firebase**

   O plugin `google-services` exige o arquivo `app/google-services.json`. 
   Coloque o arquivo do projeto Firebase na raiz do módulo `app/`.

> [!WARNING]
> O `google-services.json` **não deve** ser commitado. Em CI, ele é injetado via secret `GOOGLE_SERVICES_JSON`.

3. **Compilação e testes**

   ```bash
   # Rodar testes unitários
   ./gradlew testDebugUnitTest

   # Gerar o APK de debug
   ./gradlew assembleDebug
   ```

   O APK será gerado em `app/build/outputs/apk/debug/`.

## CI/CD

O pipeline de **Continuous Integration** (`.github/workflows/ci.yml`) roda em PRs e pushes para as branches principais e executa:

1. Build do projeto
2. Testes unitários (`testDebugUnitTest`)
3. Geração do APK de debug
4. Upload de relatórios de teste e do APK como artefatos

> É necessário configurar o secret **`GOOGLE_SERVICES_JSON`** no repositório (no GitHub: *Settings → Secrets → Actions*).

## Testes

O projeto possui testes unitários de repositório (ver pasta `app/src/test`):

```bash
./gradlew testDebugUnitTest
```

## Logs e tratamento de erros

Os fluxos de autenticação usam eventos padronizados no Logcat. Durante o
desenvolvimento, filtre as tags do aplicativo com:

```bash
adb logcat -s ARGOS_AUTH ARGOS_LOGIN ARGOS_FIRST ARGOS_RESET ARGOS_SESSION
```

Os eventos seguem o formato `event=nome chave=valor`. Senhas, tokens, códigos
de redefinição, emails e outras credenciais nunca devem ser registrados.

Falhas técnicas são convertidas em `AuthErrorCode` antes de chegar à UI. O
ViewModel publica o código; a rota resolve a mensagem em `strings.xml` e
exibe o feedback adequado. Não use `Throwable.message` diretamente na tela.

## Contribuindo

Siga as boas práticas de contribuição descritas no arquivo **[CONTRIBUTING.md](CONTRIBUTING.md)**.

## Padrão de commits

Este repositório usa **[Conventional Commits](https://www.conventionalcommits.org)** via **commitlint** + **husky**. Exemplos:

```text
feat(auth): adiciona login com Google
fix(auth): corrige validação do token
docs(readme): atualiza instruções de setup
```

## Recursos

- [Documentação oficial do Firebase para Android](https://firebase.google.com/docs/android/setup)
- [Jetpack Compose](https://developer.android.com/develop/ui/compose)
- [Conventional Commits](https://www.conventionalcommits.org)
