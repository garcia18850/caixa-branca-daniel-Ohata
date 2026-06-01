package login;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class User {
    
    // Encapsulamento: Variáveis de estado não devem ser públicas ou globais dessa forma, 
    // mas foram mantidas privadas com Getters para manter a compatibilidade lógica.
    private String nome = "";
    private boolean result = false;

    /**
     * Estabelece a conexão com o banco de dados.
     * Melhoria: Lançamento de exceção em vez de ocultá-la em um catch vazio.
     * Melhoria: Remoção do Class.forName obsoleto.
     */
    private Connection conectarBD() throws SQLException {
        // Melhoria de Segurança: Em um projeto real, as credenciais devem vir de variáveis de ambiente
        String url = "jdbc:mysql://127.0.0.1/test";
        String user = "lopes"; // Apenas para fins educacionais, ideal é externalizar
        String password = "123"; 
        
        return DriverManager.getConnection(url, user, password);
    }

    /**
     * Verifica as credenciais do usuário.
     * Melhoria: PreparedStatement para evitar SQL Injection.
     * Melhoria: Try-with-resources para evitar memory leaks (fechamento automático de conexões).
     */
    public boolean verificarUsuario(String login, String senha) {
        // Instrução SQL parametrizada
        String sql = "SELECT nome FROM usuarios WHERE login = ? AND senha = ?";
        
        // Resetando o estado para evitar contaminação entre chamadas sucessivas
        this.result = false;
        this.nome = "";

        // Try-with-resources garante que Connection, PreparedStatement e ResultSet sejam fechados
        try (Connection conn = conectarBD();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            // Atribuição de parâmetros protegida contra SQL Injection
            pstmt.setString(1, login);
            pstmt.setString(2, senha);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    this.result = true;
                    this.nome = rs.getString("nome");
                }
            }
        } catch (SQLException e) {
            // Tratamento adequado: logar a exceção ou repassar a mensagem de erro
            System.err.println("Erro ao verificar usuário: " + e.getMessage());
        }
        return this.result;
    }

    public String getNome() {
        return nome;
    }

    public boolean isResult() {
        return result;
    }
}