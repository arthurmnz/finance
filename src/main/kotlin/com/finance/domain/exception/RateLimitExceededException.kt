package com.finance.domain.exception

class RateLimitExceededException(
    message: String = "Muitas tentativas. Tente novamente mais tarde.",
) : RuntimeException(message)
