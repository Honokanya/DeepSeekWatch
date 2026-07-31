package com.honoka.deepseekwatch.data

import fi.iki.elonen.NanoHTTPD

/**
 * 二维码导入服务器：随机端口（构造传入 0），URL 带一次性 token。
 * 仅接受 /input（表单页）与 /submit（提交），其余 403/404。
 * 成功导入后由调用方调用 stop() 关闭。
 */
class QrImportServer(
    private val token: String,
    private val onKeyReceived: (name: String, key: String) -> Unit
) : NanoHTTPD(0) {

    var imported: Boolean = false
        private set

    override fun serve(session: IHTTPSession): Response {
        val t = session.parameters["t"]?.firstOrNull()
        if (t != token) return newFixedLengthResponse(Response.Status.FORBIDDEN, MIME_PLAINTEXT, "forbidden")
        return when (session.uri) {
            "/input" -> newFixedLengthResponse(
                Response.Status.OK,
                "text/html; charset=utf-8",
                INPUT_HTML.replace("__TOKEN__", token)
            )
            "/submit" -> {
                // NanoHTTPD 不会自动解析 POST body，必须显式调用 parseBody
                try {
                    session.parseBody(HashMap())
                } catch (_: Exception) {
                    // 解析失败按格式错误处理
                }
                val name = session.parameters["name"]?.firstOrNull().orEmpty()
                val key = session.parameters["key"]?.firstOrNull().orEmpty()
                if (key.isBlank() || !key.startsWith("sk-")) {
                    newFixedLengthResponse(Response.Status.BAD_REQUEST, "text/html; charset=utf-8", ERROR_HTML)
                } else {
                    imported = true
                    onKeyReceived(name, key)
                    newFixedLengthResponse(Response.Status.OK, "text/html; charset=utf-8", SUCCESS_HTML)
                }
            }
            else -> newFixedLengthResponse(Response.Status.NOT_FOUND, MIME_PLAINTEXT, "not found")
        }
    }

    companion object {
        const val MIME_PLAINTEXT = "text/plain"

        val INPUT_HTML = """
            <!DOCTYPE html><html><head><meta charset="utf-8">
            <meta name="viewport" content="width=device-width, initial-scale=1">
            <title>DeepSeek Watch</title>
            <style>
              body{font-family:system-ui;margin:24px;background:#0d1117;color:#e6edf3}
              h2{margin-bottom:4px} input{display:block;width:100%;padding:12px;margin:8px 0;
              border:1px solid #30363d;border-radius:8px;background:#161b22;color:#e6edf3;font-size:16px}
              button{width:100%;padding:14px;background:#238636;color:#fff;border:none;border-radius:8px;font-size:16px}
              .hint{font-size:13px;color:#8b949e}
            </style></head><body>
            <h2>DeepSeek Watch</h2>
            <p class="hint">输入 API Key 导入到手表（提交后请关闭本页）</p>
            <form method="POST" action="/submit?t=__TOKEN__">
              <input name="name" placeholder="名称（可选）" autocomplete="off">
              <input name="key" placeholder="sk-..." required autocomplete="off" autocapitalize="off" spellcheck="false">
              <button type="submit">导入到手表</button>
            </form>
            </body></html>
        """.trimIndent()

        val SUCCESS_HTML = """
            <!DOCTYPE html><html><head><meta charset="utf-8">
            <meta name="viewport" content="width=device-width, initial-scale=1"></head><body>
            <h2 style="color:#4CAF50">✓ 导入成功</h2>
            <p>Key 已保存到手表，可以关闭本页。</p>
            </body></html>
        """.trimIndent()

        val ERROR_HTML = """
            <!DOCTYPE html><html><head><meta charset="utf-8">
            <meta name="viewport" content="width=device-width, initial-scale=1"></head><body>
            <h2 style="color:#f85149">导入失败</h2>
            <p>Key 格式不正确（应以 sk- 开头），请返回重试。</p>
            </body></html>
        """.trimIndent()
    }
}
