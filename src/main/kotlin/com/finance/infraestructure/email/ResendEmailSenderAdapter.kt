package com.finance.infraestructure.email

import com.finance.application.port.EmailSenderPort
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient

@Component
class ResendEmailSenderAdapter(
    @Value("\${resend.api-key:}")
    private val apiKey: String,
    @Value("\${resend.from-email:onboarding@resend.dev}")
    private val fromEmail: String,
    restClientBuilder: RestClient.Builder,
) : EmailSenderPort {

    private val logger = LoggerFactory.getLogger(ResendEmailSenderAdapter::class.java)

    private val restClient = restClientBuilder
        .baseUrl("https://api.resend.com")
        .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
        .build()

    override fun sendPasswordResetEmail(toEmail: String, recipientName: String, resetLink: String) {
        if (apiKey.isBlank()) {
            logger.warn(
                "Resend API key não configurada (RESEND_API_KEY vazia). E-mail de redefinição de senha para '{}' não foi enviado externamente.",
                toEmail
            )
            return
        }

        val safeName = escapeHtml(recipientName)
        val htmlBody = buildEmailHtml(safeName, resetLink)

        val payload = mapOf(
            "from" to fromEmail,
            "to" to listOf(toEmail),
            "subject" to "Redefinição de Senha - Finance",
            "html" to htmlBody
        )

        try {
            val response = restClient.post()
                .uri("/emails")
                .header(HttpHeaders.AUTHORIZATION, "Bearer $apiKey")
                .body(payload)
                .retrieve()
                .toBodilessEntity()

            if (response.statusCode.is2xxSuccessful) {
                logger.info("E-mail de redefinição de senha enviado com sucesso para '{}'.", toEmail)
            } else {
                logger.warn(
                    "Resposta não-sucesso do Resend ao enviar e-mail para '{}'. Status: {}",
                    toEmail,
                    response.statusCode
                )
            }
        } catch (ex: Exception) {
            logger.error("Falha ao comunicar com a API do Resend para enviar e-mail a '{}'", toEmail, ex)
        }
    }

    private fun escapeHtml(text: String): String {
        return text
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#39;")
    }

    private fun buildEmailHtml(name: String, resetLink: String): String {
        return """
            <!DOCTYPE html>
            <html lang="pt-BR">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>Redefinição de Senha</title>
                <style>
                    body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; background-color: #0b0f19; color: #f3f4f6; margin: 0; padding: 24px; }
                    .container { max-width: 540px; margin: 0 auto; background-color: #111827; border: 1px solid #1f2937; border-radius: 16px; padding: 32px; }
                    .header { text-align: center; margin-bottom: 24px; }
                    .title { font-size: 22px; font-weight: 700; color: #ffffff; margin-top: 12px; margin-bottom: 8px; }
                    .content { font-size: 15px; line-height: 1.6; color: #9ca3af; }
                    .button-container { text-align: center; margin: 32px 0; }
                    .btn { display: inline-block; background-color: #2563eb; color: #ffffff !important; text-decoration: none; font-weight: 600; font-size: 14px; padding: 12px 28px; border-radius: 10px; }
                    .notice { font-size: 13px; color: #6b7280; border-top: 1px solid #1f2937; padding-top: 20px; margin-top: 28px; }
                    .link-alt { word-break: break-all; color: #3b82f6; font-size: 12px; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <div class="title">Recuperação de Senha</div>
                    </div>
                    <div class="content">
                        <p>Olá, <strong>$name</strong>,</p>
                        <p>Recebemos uma solicitação para redefinir a senha da sua conta no <strong>Finance</strong>.</p>
                        <p>Para prosseguir e cadastrar uma nova senha, clique no botão abaixo:</p>
                        <div class="button-container">
                            <a href="$resetLink" class="btn" target="_blank" rel="noopener noreferrer">Redefinir minha senha</a>
                        </div>
                        <p>Este link é de <strong>uso único</strong> e expira em <strong>30 minutos</strong>.</p>
                        <p>Caso você não tenha solicitado a alteração de senha, ignore este e-mail. Sua conta permanece em total segurança.</p>
                    </div>
                    <div class="notice">
                        <p>Se o botão acima não funcionar, copie e cole o link a seguir no seu navegador:</p>
                        <p class="link-alt">$resetLink</p>
                    </div>
                </div>
            </body>
            </html>
        """.trimIndent()
    }
}
