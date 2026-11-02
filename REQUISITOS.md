# Demandas

- [Controle de Acesso](app/src/main/java/com/android/argosapp/data/repository/FirebaseRepository.kt)
- [Uso de recurso de hardware do dispositivo (câmera, telefone ou SMS)](app/src/main/java/com/android/argosapp/view/fragment/ProfileFragment.kt)
- [Integração e consumo de API](app/src/main/java/com/android/argosapp/data/repository/RemoteArgosApiRepository.kt)
- [Persistência de dados com Firebase ou SQLite](app/src/main/java/com/android/argosapp/data/cache/db/ArgosCacheDatabase.kt)
- [Chatbot operacional](app/src/main/java/com/android/argosapp/data/repository/AiChatApiRepository.kt)
- [Configuração de perfil e avatar](app/src/main/java/com/android/argosapp/view/fragment/ProfileFragment.kt)
- [Uso de notificações no app](app/src/main/java/com/android/argosapp/data/service/NotificationService.kt)
- [Adaptação para celular e tablet](app/src/main/res/layout)
- [Uso do GPS do dispositivo](app/src/main/java/com/android/argosapp/data/repository/MapLocationRepository.kt)

## Chatbot operacional

O ChatTobby oferece criação de conversas, histórico paginado, envio de mensagens e respostas estruturadas, além de recuperação de interações assíncronas após interrupções. O app mantém um repositório remoto para a API de IA e um repositório mock para desenvolvimento.

- Interface e navegação: [ChatbotFragment](app/src/main/java/com/android/argosapp/view/fragment/ChatbotFragment.kt), [ChatConversationFragment](app/src/main/java/com/android/argosapp/view/fragment/ChatConversationFragment.kt), [ChatNewConversationFragment](app/src/main/java/com/android/argosapp/view/fragment/ChatNewConversationFragment.kt) e [ChatHistoryFragment](app/src/main/java/com/android/argosapp/view/fragment/ChatHistoryFragment.kt).
- Estado e regras do fluxo: [ChatbotViewModel](app/src/main/java/com/android/argosapp/viewmodel/ChatbotViewModel.kt).
- Contrato de dados e acesso à IA: [AiChatRepository](app/src/main/java/com/android/argosapp/data/repository/AiChatRepository.kt) e [AiChatApiRepository](app/src/main/java/com/android/argosapp/data/repository/AiChatApiRepository.kt).
- Implementação para desenvolvimento: [MockedAiChatRepository](app/src/main/java/com/android/argosapp/data/repository/MockedAiChatRepository.kt).
- Persistência e recuperação de requisições pendentes: [ChatCacheStorage](app/src/main/java/com/android/argosapp/data/cache/storage/ChatCacheStorage.kt) e [PendingInteraction](app/src/main/java/com/android/argosapp/data/repository/PendingInteraction.kt).
