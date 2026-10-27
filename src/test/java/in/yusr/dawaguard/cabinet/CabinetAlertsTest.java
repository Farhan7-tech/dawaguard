package in.yusr.dawaguard.cabinet;

import in.yusr.dawaguard.cdsco.CdscoRow;
import in.yusr.dawaguard.cdsco.FlaggedBatch;
import in.yusr.dawaguard.cdsco.FlaggedBatchRepository;
import in.yusr.dawaguard.cdsco.ListType;
import in.yusr.dawaguard.check.StripReading;
import in.yusr.dawaguard.check.VerdictEngine;
import in.yusr.dawaguard.config.DawaGuardProperties;
import in.yusr.dawaguard.whatsapp.WhatsAppClient;
import in.yusr.dawaguard.whatsapp.WhatsAppUser;
import in.yusr.dawaguard.whatsapp.WhatsAppUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CabinetAlertsTest {

    private static final String PHONE = "919800000000";

    private CabinetItemRepository items;
    private WhatsAppClient whatsapp;
    private WhatsAppUser user;
    private CabinetAlerts alerts;
    private CabinetItem pansid;
    private CabinetItem paracetamol;

    @BeforeEach
    void setUp() {
        items = mock(CabinetItemRepository.class);
        whatsapp = mock(WhatsAppClient.class);
        when(whatsapp.canSend()).thenReturn(true);
        user = mock(WhatsAppUser.class);
        when(user.getLanguage()).thenReturn("en");
        WhatsAppUserRepository users = mock(WhatsAppUserRepository.class);
        when(users.findById(PHONE)).thenReturn(Optional.of(user));

        pansid = CabinetItem.of(PHONE, new StripReading("Pansid-40", "Pantoprazole Gastro-resistant Tablets IP",
                null, "TG25-1814A", null, "SEP.2027", null, true, "clear", null), "Papa");
        paracetamol = CabinetItem.of(PHONE, StripReading.manual("Demopar 500", "Paracetamol Tablets IP", "DG26B047", null), null);
        when(items.findAll()).thenReturn(List.of(pansid, paracetamol));

        DawaGuardProperties props = new DawaGuardProperties(null, null, null, null,
                new DawaGuardProperties.WhatsApp(true, "https://graph.example", "1", "token", "v", "s",
                        "dawaguard_batch_alert", "en"), null);
        alerts = new CabinetAlerts(items, new VerdictEngine(mock(FlaggedBatchRepository.class)), whatsapp, users, props);
    }

    @Test
    void alertsTheFamilyOnceWhenANewListFlagsASavedBatch() {
        when(user.insideReplyWindow()).thenReturn(true);
        List<FlaggedBatch> newList = List.of(nsq("Pantoprazole Gastro Resistant Tablets IP", "Pansid-40 Tablets", "TG25-1814A"));

        assertThat(alerts.recheck(newList)).isEqualTo(1);
        ArgumentCaptor<String> text = ArgumentCaptor.forClass(String.class);
        verify(whatsapp).sendText(eq(PHONE), text.capture());
        assertThat(text.getValue())
                .startsWith("⚠️ *DawaGuard alert*")
                .contains("*Pansid-40* (Papa), batch *TG25-1814A*")
                .contains("failed a government lab quality test")
                .contains("Reported by LUCKNOW, CDSCO alert OCT-2026.")
                .endsWith("talk to your doctor or pharmacist before changing anything.");

        assertThat(alerts.recheck(newList)).isZero();
        verify(whatsapp, times(1)).sendText(anyString(), anyString());
    }

    @Test
    void usesTheApprovedTemplateOutsideTheReplyWindow() {
        when(user.insideReplyWindow()).thenReturn(false);
        alerts.recheck(List.of(nsq("Pantoprazole Tablets", "Pansid-40", "TG251814A")));
        verify(whatsapp).sendTemplate(PHONE, "dawaguard_batch_alert", "en", List.of("Pansid-40", "TG25-1814A"));
        verify(whatsapp, never()).sendText(anyString(), anyString());
    }

    @Test
    void sameBatchCodeOnAnotherProductDoesNotAlert() {
        when(user.insideReplyWindow()).thenReturn(true);
        assertThat(alerts.recheck(List.of(nsq("Cetirizine Tablets IP", "Cetrizet", "DG26B047")))).isZero();
        verify(whatsapp, never()).sendText(anyString(), anyString());
        verify(whatsapp, never()).sendTemplate(anyString(), anyString(), anyString(), anyList());
    }

    private static FlaggedBatch nsq(String product, String brand, String batchNo) {
        return FlaggedBatch.from(new CdscoRow(product, brand, batchNo, "Jan-2025", "Dec-2027", "Some Pharma",
                "Does not conform", "State lab", "LUCKNOW", "OCT-2026", null), ListType.NSQ);
    }
}
