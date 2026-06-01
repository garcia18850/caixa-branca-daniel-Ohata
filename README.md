# Relatório Técnico: Análise e Revisão de Código

## 1. Introdução
O objetivo desta atividade é realizar a análise estática e estrutural de um código-fonte Java legado responsável pela autenticação de usuários em um banco de dados MySQL. O escopo da atividade envolve a identificação de métricas de qualidade (como Complexidade Ciclomática e Caminhos Básicos), a detecção de vulnerabilidades críticas e a refatoração completa do código aplicando boas práticas de Engenharia de Software.

## 2. Análise Estática do Código
Durante a inspeção do código original, foram identificados os seguintes pontos críticos:

* **Documentação:** Inexistente. O código não possui Javadoc ou comentários que expliquem o propósito dos métodos (com exceção de um comentário redundante `//INSTRUÇÃO SQL`).
* **Nomenclatura:** Regular. Os nomes de métodos e variáveis são compreensíveis, mas o uso da classe genérica `User` para agrupar conexão com banco e lógica de negócio fere o princípio de Responsabilidade Única (SRP).
* **Legibilidade:** Média. A concatenação de strings para a montagem do SQL prejudica a leitura e manutenção.
* **Tratamento de Exceções:** Inadequado e perigoso. Os blocos `catch (Exception e) { }` estão vazios (conhecido como *Exception Swallowing*), ocultando falhas de conexão ou erros de SQL do sistema e do usuário.
* **Segurança (Vulnerabilidades):** Crítica. 
  1. **SQL Injection:** A concatenação direta de variáveis (`login` e `senha`) na query SQL permite que um invasor altere a lógica da consulta e faça login indevido (ex: enviando `' OR '1'='1`).
  2. **Hardcoded Credentials:** A URL do banco, usuário e senha estão expostos em texto plano no código-fonte.
* **Conexões e Boas Práticas:** O código não gerencia os recursos corretamente. Os objetos `Connection`, `Statement` e `ResultSet` são abertos, mas nunca são fechados (`.close()`), o que gera vazamento de recursos (*Memory/Resource Leaks*). Além disso, variáveis de classe (`nome`, `result`) são `public`, quebrando o pilar do encapsulamento.

## 3. Grafo de Fluxo
<img width="442" height="1672" alt="Drawio" src="https://github.com/user-attachments/assets/59513816-0d2f-4d7c-b50e-cdc27a512b1d" />

.

**Mapeamento dos Nós:**
* **Nó 1 (Início):** Declaração de variáveis, obtenção da conexão e montagem da String SQL.
* **Nó 2 (Tentativa de Execução):** Início do bloco `try`, criação do `Statement` e execução da query.
* **Nó 3 (Decisão Lógica):** Avaliação da condição `if (rs.next())`.
* **Nó 4 (Fluxo Verdadeiro):** Atribuição de valores (`result = true`, `nome = ...`).
* **Nó 5 (Tratamento de Erro):** Bloco `catch (Exception e)` caso haja falha na execução.
* **Nó 6 (Fim):** Retorno da variável `result` e fim do método.

## 4. Complexidade Ciclomática
Para o cálculo, consideramos o método `verificarUsuario`.
* **N (Número de nós):** 6
* **E (Número de arestas):** 7 (N1->N2, N1->N5 [caso a conexão venha nula e gere NullPointerException no try], N2->N3, N2->N5, N3->N4, N3->N6, N4->N6, N5->N6)
* **P (Componentes conectados):** 1

A fórmula da complexidade ciclomática é:
$$V(G) = E - N + 2P$$

**Cálculo:**
$$V(G) = 7 - 6 + 2(1)$$
$$V(G) = 1 + 2$$
$$V(G) = 3$$

O valor final obtido para a complexidade ciclomática é **3**, o que indica um código de baixa complexidade, mas que possui ramificações devido ao tratamento de erro e à verificação condicional do banco de dados.

## 5. Caminhos Básicos
Com base na complexidade ciclomática de 3, temos **3 caminhos básicos independentes**:

* **Caminho 1: Usuário encontrado com sucesso** (Fluxo feliz)
  * **Trajeto:** N1 → N2 → N3 → N4 → N6
  * **Descrição:** O método inicializa, entra no `try`, executa a query sem erros. O `rs.next()` retorna `true` (usuário existe), as variáveis de sucesso são preenchidas, pula o catch e retorna `true`.
  * **Caso de Teste:** Inserir um `login` e `senha` válidos e existentes no banco de dados.
* **Caminho 2: Usuário não encontrado** (Fluxo alternativo)
  * **Trajeto:** N1 → N2 → N3 → N6
  * **Descrição:** O método inicializa, executa a query sem erros no banco, porém o `rs.next()` retorna `false` (credenciais inválidas). O bloco `if` é ignorado, pula o catch e retorna o valor padrão `false`.
  * **Caso de Teste:** Inserir um `login` ou `senha` incorretos.
* **Caminho 3: Ocorrência de Exceção** (Fluxo de erro)
  * **Trajeto:** N1 → N2 → N5 → N6
  * **Descrição:** Ocorre uma falha durante a tentativa de criar o statement ou executar a query (ex: sintaxe SQL errada ou falta de conexão). O fluxo cai no bloco `catch`, e em seguida retorna `false`.
  * **Caso de Teste:** Simular a queda do banco de dados ou forçar uma string SQL inválida.

## 6. Melhorias Implementadas
Na versão refatorada, as seguintes melhorias foram aplicadas:
1. **PreparedStatement:** Substituiu o `Statement` convencional, resolvendo a vulnerabilidade crítica de **SQL Injection**.
2. **Try-with-resources:** Adicionado para garantir o fechamento automático das conexões (`Connection`, `PreparedStatement` e `ResultSet`), prevenindo vazamento de recursos.
3. **Tratamento de Exceções Real:** O bloco `catch` vazio foi substituído por uma impressão da stack/mensagem de erro, evitando o "silenciamento" das falhas.
4. **Encapsulamento:** As variáveis `nome` e `result` deixaram de ser globais públicas (`public`) e passaram a ser privadas (`private`) com métodos de acesso (Getters).
5. **Limpeza de código:** Remoção da chamada legada e desnecessária `Class.forName("...").newInstance()`.

## 7. Conclusão
A revisão estrutural evidenciou que códigos aparentemente simples e funcionais podem abrigar vulnerabilidades de segurança severas e problemas de gerenciamento de recursos. A aplicação do teste estrutural (identificação de nós, arestas e caminhos básicos) facilitou o mapeamento mental de como o código se comporta sob diferentes cenários (sucesso, falha lógica, falha de infraestrutura).

A maior dificuldade costuma ser identificar pontos de exceção ocultos pelo mau uso de blocos `catch` vazios. O impacto desta revisão é direto na **segurança e resiliência** da aplicação: ao aplicar o `PreparedStatement` e o correto gerenciamento de conexões, a qualidade do software foi elevada, demonstrando que a manutenibilidade e a segurança devem ser planejadas desde a construção de consultas básicas.
