# Integração do ChatTobby com a API de IA

O aplicativo mantém o login atual do backend Argos e usa o usuário autenticado no
Firebase para abrir uma sessão independente na API de IA. O Firebase ID token é
enviado somente a `POST /v1/auth/firebase`; a resposta `access_token` é mantida
em memória e enviada como Bearer nas rotas de sessão. Nenhum UID, e-mail,
permissão ou fazenda é enviado pelo aplicativo como autorização.
O histórico exibido vem das sessões da API de IA; conversas antigas que existam
somente no armazenamento local de desenvolvimento não são importadas para o
servidor.

O endereço da API é configurado pela propriedade Gradle `argosAiBaseUrl`. Para
executar o backend local pelo emulador Android, o padrão é
`http://10.0.2.2:8080/`, que corresponde à porta publicada pelo Compose. O
manifesto de depuração permite HTTP para esse endereço local. Para homologação e
produção, forneça o endereço HTTPS real ao compilar, por exemplo:

```powershell
.\gradlew.bat :app:assembleRelease -PargosAiBaseUrl=https://<host-da-api>/
```

O host de produção da API de IA ainda precisa ser informado pela implantação;
não é inferido a partir do endereço do backend Spring Boot. Para um dispositivo
físico na rede local, substitua `10.0.2.2` pelo endereço acessível do computador
que executa o serviço.

Ao receber `401` em uma rota protegida, o cliente força a atualização do Firebase
ID token, faz uma nova troca em `/v1/auth/firebase` e repete a chamada original
uma única vez. Respostas `403` e demais erros não provocam renovação automática.
O token próprio não é persistido. A troca de usuário ou logout limpa o token em
memória e o histórico visual ativo; sessões e mensagens são recuperadas da API.

Mensagens comuns usam `client_message_id`. Perguntas são respondidas com o
`interaction_id`, `question_id` e a alternativa estruturada por `input_type`:
texto, `option_id` ou confirmação booleana. O histórico usa cursor e preserva
perguntas, opções, respostas, marcação de conteúdo redigido e o status de resposta
parcial retornados pela API.
