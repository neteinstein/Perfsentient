package org.neteinstein.perfsentient.core.domain.usecase

import org.neteinstein.perfsentient.core.domain.repository.ScamMessageRepository
import org.neteinstein.perfsentient.core.model.SmsPartnerStatus

/** Whether SMS Toolbox is installed, genuine, connected and checking texts. Read it again on resume. */
class GetSmsPartnerStatusUseCase(private val repository: ScamMessageRepository) {
    suspend operator fun invoke(): SmsPartnerStatus = repository.status()
}
