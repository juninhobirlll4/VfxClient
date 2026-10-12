# Como trabalhar no VfxClient

## Regras

1. **Um commit por funcionalidade.** Cada commit compila sozinho (`gradlew build` verde). A CI confere todo push.
2. **Só identidade nossa no código.** Nenhum nome, pacote, classe, texto ou asset de outro client.
   Se uma referência externa ajudou a entender um comportamento, ela fica numa issue ou nota — no código entra só a solução.
3. **Assets começam como placeholder versionado** em `branding/svg/` e são trocados depois mantendo o mesmo nome.
   Nunca entra textura emprestada.
4. **Dúvida de escopo:** abra uma issue e pergunte antes de implementar.
5. **Javadoc, comentários e docs em pt-BR.**

## Convenções

- Pacote base: `com.vfxclient` (`com.vfxclient.core` no mod, `com.vfxclient.launcher` no launcher).
- Id do mod e namespace de assets: `vfx` (`assets/vfx/...`).
- Membros adicionados por Mixin usam prefixo `vfx$` e `@Unique`.
- O mod compila para **Java 8**: sem `var`, `record`, text blocks ou APIs depois do Java 8.
- O launcher usa Java 21 normalmente.

## Mensagens de commit

```
<área>: <o que mudou>

core: HUD editável com drag-and-drop
launcher: login Microsoft
ci: publicar jar do mod no release
```

## Release

Atualize o roadmap no README, depois `git tag vX.Y.Z` e `git push origin vX.Y.Z`.
