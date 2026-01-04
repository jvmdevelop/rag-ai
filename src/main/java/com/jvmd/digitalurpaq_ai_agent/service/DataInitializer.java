package com.jvmd.digitalurpaq_ai_agent.service;

import com.jvmd.digitalurpaq_ai_agent.model.RetrievalDocument;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class DataInitializer implements CommandLineRunner {

    private final RetrievalService retrievalService;

    @Value("${pdfPath:raspisanie.pdf}")
    private String pdfPath;

    @Value("${isInit:false}")
    private boolean isInit;

    @Value("${useChunking:true}")
    private boolean useChunking;

    public DataInitializer(RetrievalService retrievalService) {
        this.retrievalService = retrievalService;
    }

    @Override
    public void run(String... args) throws Exception {
        if (isInit) {
            log.info("Data initialization skipped (isInit=true)");
            return;
        }

        log.info("=== Starting Data Initialization ===");
        log.info("Use chunking: {}", useChunking);

        long startTime = System.currentTimeMillis();

        try {
            String pdfText = loadPdfIfExists();

            List<RetrievalDocument> documents = createDocuments(pdfText);

            log.info("Created {} documents for indexing", documents.size());

            if (useChunking) {
                log.info("Saving documents with chunking...");
                retrievalService.saveAllWithChunking(documents)
                        .doOnNext(doc -> log.debug("Saved chunk: {}", doc.getId()))
                        .blockLast();
            } else {
                log.info("Saving documents without chunking...");
                retrievalService.saveAll(documents)
                        .doOnNext(doc -> log.debug("Saved document: {}", doc.getId()))
                        .blockLast();
            }

            long duration = System.currentTimeMillis() - startTime;
            long count = retrievalService.count().block();

            log.info("=== Data Initialization Completed ===");
            log.info("Total documents in index: {}", count);
            log.info("Initialization took: {}ms", duration);

        } catch (Exception e) {
            log.error("Error during data initialization", e);
            throw e;
        }
    }

    private String loadPdfIfExists() {
        try {
            File file = new File(pdfPath);
            if (!file.exists()) {
                log.warn("PDF file not found: {}", pdfPath);
                return "";
            }

            log.info("Loading PDF from: {}", pdfPath);
            PDDocument document = PDDocument.load(file);
            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(document);
            document.close();

            log.info("PDF loaded successfully, length: {} chars", text.length());
            return text;

        } catch (Exception e) {
            log.error("Error loading PDF: {}", e.getMessage());
            return "";
        }
    }

    private List<RetrievalDocument> createDocuments(String pdfText) {
        List<RetrievalDocument> documents = new ArrayList<>();


        if (!pdfText.isBlank()) {
            documents.add(new RetrievalDocument("pdf_schedule", "Расписание кружков из PDF", pdfText));
        }


        documents.add(new RetrievalDocument("schedule_bells", "Расписание звонков",
                "**РАСПИСАНИЕ ЗВОНКОВ**\n\n" +
                        "| 1 СМЕНА   |             | 2 СМЕНА   |             |\n" +
                        "| --------- | ----------- | --------- | ----------- |\n" +
                        "| 1 занятие | 9⁰⁰ - 9⁴⁰   | 1 занятие | 15⁰⁰ - 15⁴⁰ |\n" +
                        "| 2 занятие | 9⁴⁵ - 10²⁵  | 2 занятие | 15⁴⁵ - 16²⁵ |\n" +
                        "| 3 занятие | 10³⁰ - 11¹⁰ | 3 занятие | 16³⁰ - 17¹⁰ |\n" +
                        "| 4 занятие | 11¹⁵ - 11⁵⁵ | 4 занятие | 17¹⁵ - 17⁵⁵ |\n" +
                        "| 5 занятие | 12⁰⁰ - 12⁴⁰ | 5 занятие | 18⁰⁰ - 18⁴⁰ |\n" +
                        "| 6 занятие | 12⁴⁵ - 13²⁵ | 6 занятие | 18⁴⁵ - 19²⁵ |\n" +
                        "|           |             | 7 занятие | 19³⁰ - 20¹⁰ |\n" +
                        "|           |             | 8 занятие | 20¹⁵ - 20⁵⁵ |"));


        documents.add(new RetrievalDocument("main_info", "Главная страница - Описание Дворца школьников",
                "Дворец школьников занимает авангардную позицию в развитии новых, наиболее востребованных направлений ІТ-технологий. " +
                        "Приоритетным направлением является развитие проектно-исследовательской деятельности и научно-технического творчества, " +
                        "соответствующее наиболее перспективным направлениям развития современной науки и техники. " +
                        "На базе Дворца школьников функционирует IT-центр, который является местом интеллектуального развития и досуга детей и молодежи."));


        documents.add(new RetrievalDocument("directions", "Направления деятельности",
                "Научно-биологическое: Лаборатория современных биотехнологий; Кабинет Биологии; Кабинет Химии; Кабинет Гидропоники. " +
                        "IT - Информационные технологии: Лаборатория промышленного интернет вещей; Лаборатория программирования контрольно-измерительных систем; " +
                        "Кабинет 3d-прототипирования; Кабинет голографии. " +
                        "Художественно-эстетическое: Театральные кружки; Домбровые кружки; Хореографические кружки; Кружок журналистики и медиатехнологии. " +
                        "Художественная школа. Бизнес школа. Журналистика и медиатехнологии. Дебатный клуб и КВН."));


        documents.add(new RetrievalDocument("contacts", "Контакты и местоположение",
                "Местоположение: 150000, город Петропавловск, улица Жамбыла Жабаева, 55 А. " +
                        "Номер телефона: Приемная: 8 7152 34-02-40; Ресепшн: 8 7152 50-17-03. " +
                        "Email: dvorecsko@sqo.gov.kz. " +
                        "График работы: пн-пт: 09:00-20:10 (перерыв 12:00-15:00); сб-вс: 9:00-18:00."));


        documents.add(new RetrievalDocument("stats", "Статистика Дворца школьников",
                "Более 100 кружков, 80% из которых бесплатные. " +
                        "2875 учеников одновременно может посещать дворец. " +
                        "7 лет опыта работы с детьми."));


        documents.add(new RetrievalDocument("advantages", "Преимущества Дворца школьников",
                "Интересная и разнообразная школьная жизнь; " +
                        "Современные интерактивные оборудования и лаборатории; " +
                        "Открытый потенциал к новым идеям и модернизации; " +
                        "Высокий научно-педагогический потенциал; " +
                        "Уникальные образовательные программы; " +
                        "Участие в соревнованиях, конкурсах, олимпиадах."));


        documents.add(new RetrievalDocument("summer_academy", "Летняя академия",
                "Летняя Академия при Дворце школьников. Возраст детей: от 6 до 18 лет. " +
                        "6 незабываемых насыщенных сезонов: " +
                        "1 сезон - Художественная академия ART әлем 09.06-20.06.2025; " +
                        "2 сезон – Научная академия «Бионик» 23.06-04.07.2025; " +
                        "3 сезон - Академия IT технологий «Пиксель» 08.07-19.07.2025; " +
                        "4 сезон – Академия MIX «Happy holidays» 21.07.-01.08.2025; " +
                        "5 сезон - Творческая академия «Дарын life» 04.08-15.08.2025; " +
                        "6 сезон - Художественная академия ART әлем 18.08-29.08.2025. " +
                        "Стоимость: 6000 тенге. " +
                        "Обращаться по телефонам: 50-17-03, 34-02-44"));

        documents.add(new RetrievalDocument("schedule", "Расписание", "Paconcanne sanaTuii s DHTOSOHE\n" +
                "ILO —Mycnna A.M., Taiixanosa BOK.\n" +
                "\n" +
                "Ne | Bpema Tlonexenbank Bropauk Cpena Uerpepr Natanua\n" +
                "| 09,00-09,40\n" +
                "\n" +
                "2 09.45-10.25\n" +
                "\n" +
                "3 10.30-11,10\n" +
                "\n" +
                "4 11.15-11,55\n" +
                "\n" +
                "3 12,00-12.40-\n" +
                "\n" +
                "6 12,45-13,25\n" +
                "\n" +
                "1 15.00-15.40 CapcemOaesa A.O. «Xumua 6. reii-\n" +
                "\n" +
                "ake.sepTrey TOOBM\n" +
                "2 | 1545-1625 Tatixanopa 6)K HOM rp mo 6Honornn/ Mycnna A.M, H/3\n" +
                "rpyona no 6Honorun\n" +
                "3 | 16.30-17.10 CapcemOaesa A.O, «Xumua 6, rein-\n" +
                "akc.3epTTey TOGLI»\n" +
                "\n" +
                "4 17.15-17.55\n" +
                "\n" +
                "5 18.00-18.40\n" +
                "\n" +
                "6 18.45-19.25\n" +
                "\n" +
                "Ne Bppema Cy6bora\n" +
                "\n" +
                "i 9.00 -9.40\n" +
                "\n" +
                "2 09.45 -10,.25\n" +
                "\n" +
                "3 10.30 -L1,10\n" +
                "\n" +
                "4 LL.15- 11.55\n" +
                "\n" +
                "3 12.00 — 12.40\n" +
                "\n" +
                "6 12.45 — 13,25\n" +
                "\n" +
                "1 14.00 - 14.40\n" +
                "2 14.45 — 15.25\n" +
                "\n" +
                "3 15.30 — 16,10\n" +
                "4 16.15 ~ 16.55\n" +
                "3apegyroiad Hay4Ho-buonorn4eckHM HanpaBileHHem if Bopoosega JI.B.\n" +
                ", f\n" +
                "\n" +
                "\fPacnucanne 3anATnit & kaGunete HNTepHer Bemell\n" +
                "T1110 —Aypxenora J. I., Hermerxanosa I, 11,\n" +
                "\n" +
                "Me | Bpema Tloneqenbank Bropank Cpeaa Uetpepr Tatnuua\n" +
                "\n" +
                "1 09.00-09.40\n" +
                "\n" +
                "2 09.45-10.25\n" +
                "\n" +
                "3 10.30-11.10\n" +
                "\n" +
                "4 11, 15-11.55\n" +
                "\n" +
                "5 12.00-12,40\n" +
                "\n" +
                "6 12.45-13.25\n" +
                "\n" +
                "i 15.00-15,40 Hypxexona JIT I ton.\"Ka3axcran: Hypxenona JIP | ton. \"Kasaxcrau: AHeamemacanoea PEL\n" +
                "TapHXnl McH Gow.» (busuKa a6.) TapHxb! Mex Som» (CIP) «A3byxa Kpaesedenuan| ep\n" +
                "\n" +
                "2 15,45-16,25 Hypxexosa JIT I ton.\" Kaszaxeran: Hypxenopa JID I ron. \"Kasakcran: Heememacanoea TL\n" +
                "TapHxsl Mex Goi.» (husuKa na6.) TapHxbl Men Gam» (CJIP) «A36yea xpaesedenuanl 2p\n" +
                "\n" +
                "3 16.30-17.10 Hypkenosa JIT II ton.\"Kagaxetax: Hypkenosa JID II ton.\"Kasakctan:\n" +
                "‘TapHXbI McH 601. TapHxe! MeH Gor.»\n" +
                "\n" +
                "4 17.15-17.55 Hypkenona JIT II ton.\"Kagaxcran: Hypkenosa JIT I tom.\" Kagakeran:\n" +
                "Tapuxel Mex Gon.» TAPHXBI MeH 60/1.»\n" +
                "\n" +
                "5 18.00-18.40 Hypkenosa JIT II ton,\"Kasaxctar: Hypkenosa JIV IM ton.\"Kasakeran:\n" +
                "TapHxe! Mex Gon.» Tapuxei MeH Bou.»\n" +
                "\n" +
                "6 18.45-19.25 Hypkenosa JIT Il ton.\"Ka3axctan: Hypxenosa JIV Ill ton.\"Kasaxeran:\n" +
                "Tapuxs! Men Gon.» TapHxel Mex Gost.»\n" +
                "\n" +
                "zi 19.30-20.10\n" +
                "\n" +
                "Ne Bpema Cy66ora\n" +
                "\n" +
                "1 9.00 -9.40\n" +
                "\n" +
                "iz 09.45 -10,25\n" +
                "\n" +
                "3 10.30 -11.10\n" +
                "\n" +
                "4 L115 - 11.55\n" +
                "\n" +
                "5 12.00 — 12.40\n" +
                "\n" +
                "6 12.45 ~ 13.25\n" +
                "\n" +
                "1 14.00 — 14.40\n" +
                "\n" +
                "2 14.45 — 15.25\n" +
                "\n" +
                "3 15.30 — 16.10\n" +
                "\n" +
                "4 16.15 — 16.55\n" +
                "\n" +
                "Sapenyiollad HaydHo-GuonornyeckHM HanpaBsieHHem\n" +
                "\n" +
                "Bopo6teza JIB.\n" +
                "\n" +
                "Sau YP\n" +
                "\n" +
                "A\n" +
                "\n" +
                "\fPacnucanne janaTuii 8 .abopaTopun coppemeHHbix GuoTexHowornit\n" +
                "TIJ1O —IMokaera A.C., Mycuna A.M., [lakenona A.M., [nméepren H.b., Kenxebaera 3.6.\n" +
                "\n" +
                "Ne | Bpema Tlonenenbunk BropHuk Cpena Yersepr TatHnita\n" +
                "\n" +
                "I 09.00-09.40\n" +
                "\n" +
                "2 09.45-10.25\n" +
                "\n" +
                "x 10,30-11.10\n" +
                "\n" +
                "4 11.15-11.55\n" +
                "\n" +
                "i) 12,00-12.40\n" +
                "\n" +
                "6 12.45-13.25\n" +
                "\n" +
                "| 15,00-15.40 | Capcem6aena AO. «Xumua 6 Ioxaesa AC Taburar Senrinepi\n" +
                "FbUI-9KC 3epTTey TO6L»\n" +
                "\n" +
                "2 15.45-16.25 | Capcem6aena A.O. «Kumua 6. Woxaepa AC Taburat Genrinepi Kenaxe6aena 36 Buonorna 6-ma Kerwxe6aena 36 Buonorua 6-ma r.\n" +
                "FbIN-9KC3epTTey TOOBIn F. 9KC2KoHe 3epTTey TOGsI 9KC.KOHE 3epTTey TOGsI\n" +
                "\n" +
                "3 16,30-17.10 | Illaxenosa AM 3uatoKu Iakerosa AM 3naToKku Guonorue 3 rp Kenxe6aera 36 Buonorua 6-ma Kerxe6aesa 36 Bronorna 6-104 r.\n" +
                "Guomornu | rp F, 9KC.2K9He 3epTTey TOObI 3KC.KoHe 3epTTey TOOBI\n" +
                "\n" +
                "4 17.15-17.55 | LWlaxenopa AM 3xatoKu Ilaxenosa AM 3natoKn 6uonorne 3 rp Iaxenosa AM 3natoxa Guonorun | rp\n" +
                "Suonornn | rp\n" +
                "\n" +
                "5 18.00-18.40 | [pn6epren HB Iakenosa AM 3natoxn Guonorun | rp\n" +
                "«AWH@aaarel XHMAAY\n" +
                "\n" +
                "6 18.45-19.25 | UbinGepres HB Iakenosa AM 3uatoxu Guonoruu 2 rp\n" +
                "«ARHaTaANaF bl XHMHAY\n" +
                "\n" +
                "7 1930-2010 Mlakenopa AM 3uatoxu 6uonorun 2 rp\n" +
                "\n" +
                "8 | 20,15-20.55\n" +
                "\n" +
                "Ne Bpema CyG6ota\n" +
                "\n" +
                "1 9.00 -9.40\n" +
                "\n" +
                "2 09.45 -10.25\n" +
                "\n" +
                "3 10.30 -11.10 Mycnua A.M. y/o rpynna mo Guonornu\n" +
                "\n" +
                "4 LL.ES- 11.55 Mycuna A.M. 4/5 rpynna no Guonorn\n" +
                "\n" +
                "5 12.00 - 12.40\n" +
                "\n" +
                "6 12.45 - 13.25\n" +
                "\n" +
                "1 14.00 - 14.40\n" +
                "\n" +
                "2 14.45 — 15.25\n" +
                "\n" +
                "a 15.30 — 16.10\n" +
                "\n" +
                "Sapeyro ian Hay4Ho-6non OPH4eckKHM HalipaBleHHemM\n" +
                "\n" +
                "Bopo6zeza JI.B.\n" +
                "\n" +
                "Jour JAP Sy\n" +
                "\n" +
                "\fPacnucanne 3anATHii B AaGoparopua GHesornn\n" +
                "\n" +
                "Lt mt Re\n" +
                "\n" +
                "ane}\n" +
                "fal i\n" +
                "\n" +
                "2025 rox\n" +
                "\n" +
                "ory\n" +
                "\n" +
                "HJLO Wymataesa B.K., Mycuna A.M, Taiianopa B, iK., [loxacea A.C., Komerannua H.b., KenxuGaesa 3.6., Wlarenona wina A,K.\n" +
                "Ne | Bpema Moneqenunk Bropauk Cpena Uersepr Tiatanga\n" +
                "1 09.00-09 40\n" +
                "2 | 09.45-1025 | Kewke6aepa 3b Kae 6uonory Kenxe6aena 3B OKac 6uonory\n" +
                "3 | 10.30-11.10 | Kemxe6acna 3B «3Kac Guonory M6anynna AK «)Kac 2Kon0r» Kerxe6aena 3B «Kac 6uonory Méagynna AK «ac\n" +
                "aKOnOry\n" +
                "\n" +
                "4 11.15-11.55 | Koweranuua LB. HOanynna AK «Kac 3konor» Koweranuua KB. Mbagyona AK @Kac\n" +
                "\n" +
                "«@nopapuym aneminge» «@nopapwym anemingen aKONOrY\n" +
                "3 12.00-12.40 | Koweranuya H.5 Koweranuna HB.\n" +
                "\n" +
                "«@nopapuym aneminnen «DropapHyM aneminaen\n" +
                "6 12.45-13.25\n" +
                "I 15.00-15.40 Kemxe6aera 36 Buonorua 6-u1a F. Ioxaesa AC Tafuear Genrinepi\n" +
                "\n" +
                "9KC2KaHe 3epTtTey TOGEI\n" +
                "\n" +
                "2 15.45-16.25 | Kenxebaesa 3b Buonorna 6-102 Fr. Kenxefaesa 36 Buonorua 6-11 F. Taitxanopa BJK Buonorua 6.From. axc IWokacea AC Taburar 6enrinepi Mycnna A.M, x/9\n" +
                "\n" +
                "3KC.KOHC 3EpTTey TOGEI 3KCKoHe 3epTrey TOOBI *OHE 3, TOORT rpyana no 6HonorHn\n" +
                "e] 16.30-17.10 | Kenxe6aezna 36 Buonorua 6-1 Fr. ’yma6aeea BK «Outi TaiiaHona BK Buonorus 6a. 3K Mycnua A.M. a/9 rpynoa no Mycuna AM. u/3\n" +
                "\n" +
                "9KC.KOHe JepTrey TOGEI Hecllegopatenuy KaHE 3, TOOKE Guonornn rpynma mo GHomOrHH\n" +
                "4 17.15-17.55 MKyma6aesa BK «3uatoKu Guonornu» dKymaGaesa BK «3HaroKn Guonorany Mycnra A.M. «BHomorna\n" +
                "\n" +
                "aneminge»\n" +
                "5 18.00-18.40 | [dfaxenosa AM 3vamoxu Guonozun 2 2p KymaGacaa BK «3naroxu Guonornun ymaGaesa BK «3HaToKu Guonorauy Mycuhna A.M. «buonorna\n" +
                "aTeMInge»\n" +
                "\n" +
                "6 18.45-19.25 | {Maxexoea AM 3namoxu 6uonozuu 2 2p\n" +
                "7 19.30-20.10\n" +
                "Ne Bpema Cy6Gora\n" +
                "it 9.00 -9.40\n" +
                "2 09.45 -10.25\n" +
                "3 10.30 -11.10 Tafpxanona BX .Buonorna 6.FBU1. 9k. KaHE 3. TOGBI\n" +
                "4 LL1S- 11.55 | Taipkarona BOK .buonorna 6.rbui. akc. KaHe 3, TOOL\n" +
                "5 12.00—12.40 | Capcem6aepa A.O. «Xumua 6. roui-aKc.2eprrey Toby\n" +
                "6 12.45-13.25 Capcem6aena A.O. «Xumua 6. PEUI-9KC-3epTrey TOGEM\n" +
                "] 14.00 - 14.40\n" +
                "2 14.45 — 15.25\n" +
                "3 15.30 - 16.10\n" +
                "4 16.15 - 16.55\n" +
                "5 17.00-17.40\n" +
                "\n" +
                "Sapenyroumas Hay4 HOo-6HOnOrHYeCKHM HanpasJieHHem\n" +
                "\n" +
                "opodnesa J1.B.\n" +
                "\n" +
                "Sac YP how\n" +
                "\n" +
                "\fPacnucanne 3anatTHii 8 aadoparopun CJ[Paqno\n" +
                "TLLO — Cyneiimenos 2K.C., Hermerxanoea II.\n" +
                "\n" +
                "z\n" +
                "\n" +
                "Bpema Tlovegemank Bropunk Cpega erpepr Tlarwnua\n" +
                "1 09.00-09.40\n" +
                "2 09,45-10,.25\n" +
                "3 10.30-11.10\n" +
                "4 11, 1S-11,55\n" +
                "5 12.00-12.40\n" +
                "6 12.45-13,25\n" +
                "1 15.00-15,40 Hermetaxanosa [LU «As6yxa Hypkeuosa JIT I ton.\"Kasaxetar:\n" +
                "kpaesegenum | rp tTapHxal MeH Gon.» (CHP)\n" +
                "2 15.45-16,25 Hermerxanopa [LI «As6yxa Hypkenosa JIT I ron.\"Ka3axetan:\n" +
                "kpacsenenua» | rp Taprxet Men Gon.» (CHP)\n" +
                "3 16.30-17.10 Hermetokanopa I'L «Asya Hermeraanosa [TI «A36yka\n" +
                "xpaesenenHay 2 rp Kpaesesenua» 2 rp\n" +
                "4 17.15-17.55 Hermerxanosa [LI «As6yKa Hermetaanopa [TL «AsGyKa Cynelimenos KC «Kazaxctau\n" +
                "kpaesegenna» 2 rp KpaeBeweHHAy 2 rp Tapuxpidax ¥BT-ra taliban»\n" +
                "5 18.00-18.40 CynelimMenos KC «Ka3akcTaH TapHXxblHan Cynefimeros JKC «Kasaxctan\n" +
                "¥BT-ra nalisingpiky Tapuxbinan ¥BT-Fa naiiptiabiKy\n" +
                "6 18.45-19,.25 Cynelimenos XKC «Kasaxotan Tapuxpinan\n" +
                "Y¥BT-ra patismniny\n" +
                "? 19,30-20.10\n" +
                "Ne BpeMa Cyé6ora\n" +
                "I 95.00 -9,40\n" +
                "2 09.45 -10.25\n" +
                "3 10.30 -11.10\n" +
                "4 LLIS- 11.55\n" +
                "5 12.00 — 12.40\n" +
                "6 12.45 — 13.25\n" +
                "1 14.00 - 14,40\n" +
                "\n" +
                "Sapenylouaa Hay4Ho-6nonornyeckH M HallipaBJieHHemM\n" +
                "\n" +
                "‘Bopodseza JI.B.\n" +
                "\n" +
                "\fPacnucanne sansTuii p sadoparopnn KHC\n" +
                "\n" +
                "TUO — Kymabaena B.K., Wlokaena A.C., Myenna A.M., Taitxanosa BK. Kenymmaosa A.K., CapcemOaena A.O., [laxenos oS u\n" +
                "\n" +
                "No | Bpema Tloneqeannk Bropark Cpeaa Uersepr Tinranna\n" +
                "\n" +
                "] 09.00-09.40\n" +
                "\n" +
                "2 09.45-10.25\n" +
                "\n" +
                "3 10.30-11.10\n" +
                "\n" +
                "4 11.15-11.55\n" +
                "\n" +
                "5 12.00-12.40\n" +
                "\n" +
                "6 12.45-13.25\n" +
                "\n" +
                "1 15.00-15.40 Talixatopa BK HSH rp no 6Honorun\n" +
                "\n" +
                "2 | 15.45-16.25 | WKenyiunnosa AK «Ons AmxuMuin WymaGaeaa BK Oust 2KymaGaeaa BK «lOuni Tainkanosa BK Buonorua 6 roti. 9Kc,\n" +
                "\n" +
                "ucenenoBaTenun HecnesopaTenHy aaHe 3. TOGEI\n" +
                "\n" +
                "3 16.30-17,10 Mycuya A.M. H/9 rpynniano GHonorun $| KymaGaesa BK «lOuprii Tafiokanopa BK Buosorus 6.Fai. 3kc.\n" +
                "HeCHeMOBATeNH» o«aHe 3. TOBBI\n" +
                "\n" +
                "4 17.15-17,55 Mycuua A.M. n/o rpynna no GHomorun Iaxenosa AM 3Juamoxu\n" +
                "\n" +
                "2 Guonozuu 3 2p\n" +
                "\n" +
                "5 18.00-18.40 Mycuna A.M, «Buonorua aneminge» Waxerona AM 3Hatokn\n" +
                "Gronorun 3 rp\n" +
                "\n" +
                "6 18.45-19.25 Mycuna A.M, «Buonorua aneminge» Kenyummtopa AK [Ueim6epren HB «Mac seprreymi»\n" +
                "Octiospr xamnn\n" +
                "\n" +
                "7 19,30-20.10 UWpm6epres HB «Kac 3eprreyui»\n" +
                "\n" +
                "Ne Bpema Cy66ora\n" +
                "\n" +
                "I 9.00 -9.40 [Wsin6epren HB «)Kac seprreyuti»\n" +
                "\n" +
                "2 09.45 -10,25 lUsie6epren Hb «@Kac 3eprreyuti»\n" +
                "\n" +
                "3 10,30 -11,10 IUbm6epren HB\n" +
                "\n" +
                "«Afivananarhl XAMHa»\n" +
                "4 LLIS- 11,55 IUbinGepren HB\n" +
                "«AHpananarel XHMHa»\n" +
                "12,00 — 12.40\n" +
                "\n" +
                "6 12.45 = 13,25\n" +
                "\n" +
                "1 14.00 - 14.40\n" +
                "\n" +
                "2 14.45 — 15.25\n" +
                "\n" +
                "3 15.30 — 16.10\n" +
                "\n" +
                "3apeqyroulaad Hay4Ho-GHONOrM4eckHM HallpaneHHeM (Ga Bopo6pesa JI.B.\n" +
                "\n" +
                "4\n" +
                "\n" +
                "au YBP ted]\n" +
                "\n" +
                "\f. ty net SACS\n" +
                "YTBEPAAAIQ «oN\n" +
                "URKIL«bopen sin KOB»>\n" +
                "Avi pexropi AGuamtas AK,\n" +
                "a\n" +
                ". of\n" +
                "Pacnucanne 3ansaTHii 6 saboparopun XHMuH\n" +
                "TL1O —CapcemOaena A.O., Kenyumsoea AK, Jhoraa O.B., aryesa C.H., Koueranuna H.B., WmGéepr\n" +
                "Se | Bpema TloneneinHnk Bropnak Cpena Uersepr flatanua\n" +
                "I 09.00-09.40\n" +
                "2 09.45-10.25\n" +
                "3 10.30-11.10 Koweratnua H.B. «Afinatameisgarsi Koweraauna 1.5.\n" +
                "XHMHH» 2 rp «ATHATaMbIa iar bt XHMUR» 2 Pp\n" +
                "4 11,15-11.55 Koweranuya 5, «Amana qaret Koweranuya HB\n" +
                "xHMHA» 2 rp CATHalaMbisgarel X4MuA» 2 rp\n" +
                "3 12.00-12.40\n" +
                "6 12.45-13.25\n" +
                "1 15.00-15.40 | Ulatyesa CH HOH rp no xumnu Wartyesaa CH HOW rp no xumun [atyesa CH H3Erp no xumeu Watyesa CH HOHrp no xumMHa Bepemaxanona BOK «OcHosst\n" +
                "XHMHH»\n" +
                "2 15.45-16.25 | Llaryesa CH H3H rp no xumun Matyesa CH HD rp no xumnn Waryesa CH HOMrp no xumua Wartyesa CH HSMrp no xuMui Bepempkanosa BK «Ocnoast\n" +
                "XHMHH»\n" +
                "3 16.30-17.10 | ‘Kenyuunopa AK «FOnpiit Bepespxanopna GK «Ochose! xHMHH»\n" +
                "AUIXHMHK»\n" +
                "4 W7A5-17.55 | Kenywunona AK 3annmatensnaa Bepespxanona BK «Ochossl XHMHH»\n" +
                "XHMUA\n" +
                "5 18.00-18.40 | 3Kenyumnona AK 3anmmatensuas Kenyumiospa AK Ocnospi XxuMnn\n" +
                "XHMHA\n" +
                "6 18.45-19.25 | Thoraa OB H9Mrp no xumnn Jhotas OB H3Mrp no xaMHH Jhotan OB H3Hrp mo xumuw Jhotas OB HOMrp no xumun\n" +
                "Es 19.30-20.10 | Jtoras OB H3Hrp no xumnn Jlrotas OB H3Mrp_ no xamMHy Jhotas OB HSMrp no xamuu Jhoraa OB H3Kirp no xumun\n" +
                "8 | 20.15-20.55\n" +
                "Ne BpemaA Cy6tota\n" +
                "1 9.00 -9.40 MKenyurmropa AK «Ousili AnxHmMuK»\n" +
                "2 09.45-10.25 | WKenyumnosa AK «FOusii Anxumuk»\n" +
                "3 10.30-11.10 | )Kenymmnora AK Ocuosst xumuu\n" +
                "4 LL1S- 1155 | )Kewymmnopa AK Ocnosst xumMnu\n" +
                "5 12.00—-1240 | dKenyumnora AK 3annmatenbnaa xHMMa\n" +
                "6 12.45-13.25 *Keny Hoga AK 3aHHMaTeobHasa XUMHA\n" +
                "1 14.00— 14.40\n" +
                "2 1445-1525\n" +
                "15,30 — 16.10\n" +
                "\n" +
                "Japesyoulad Hay4Ho-6uonormueckuM HalpaBleHHem\n" +
                "\n" +
                "Bopo6tesa JLB.\n" +
                "\n" +
                "\fPacnucanne 3anaTHit\n" +
                "Kay6a OKypek xpiatypp>\n" +
                "Ka6uner «)Kypek xBLITyEp>\n" +
                "\n" +
                "Isatanya\n" +
                "\n" +
                "Cy66otTa\n" +
                "\n" +
                "11:00-16:00\n" +
                "\n" +
                "10:00-16:00\n" +
                "\n" +
                "Sou YeP Bog,\n" +
                "\n" +
                "Lh -OJIBHHKOB>>\n" +
                "\n" +
                "\fPacnucanne 3anaTHit\n" +
                "\n" +
                "JIajopaTopHa 31eKTPOHHKH H IIeEKTPOTeXHHKH\n" +
                "ILO — Bopoénés TM.\n" +
                "\n" +
                "Ne\n" +
                "\n" +
                "| Bpema\n" +
                "\n" +
                "HoneqenbaynKk\n" +
                "\n" +
                "Bropuux Cpeaa | Yersepr [ Tariana Cy66ora\n" +
                "1 emena\n" +
                "if 09.00-09.40 l rpymitta «SaHHMatTelbnaa 1 rpyntta «3aHHMaTenbHaa\n" +
                "3NeKTpOHHNKaY aneKTpoHUKa»\n" +
                "MIO BopoGres 0M. TIO Bopodses P.M.\n" +
                "2 | 09.45-10.25 | rpyniia «3anumarenbHas 1 rpyrma «3anumMarenpyaa\n" +
                "3IEKTPOHMKan 3eKTPOHHKay\n" +
                "110 Bopobnes [.M. TLIO Bopobses T.M.\n" +
                "a 10.30-11.10 2 rpynna «SaHumarenbHan\n" +
                "3R€KTPOHHKay»\n" +
                "NO Boposses I'.M.\n" +
                "4 11.15-11,55 2 rpynna «Janumarenbyaa\n" +
                "3ICKTPOHHKa»\n" +
                "M10 Bopobses .M.\n" +
                "a 12.00-12.40 12.05-12.45\n" +
                "1 rpynna «IIpaxraueckas\n" +
                "alekTpOHHKa»\n" +
                "[110 Bopo6nes C.M.\n" +
                "6 12.45-13.25 12.50-13.30\n" +
                "1 rpynna «Ipakruueckaa\n" +
                "qieKTpOHHKa»\n" +
                "[lO Bopoébes [.M.\n" +
                "2 emena\n" +
                "1 15.00-15.40\n" +
                "2 15.45-16.25\n" +
                "3 16.30-17.10 2 rpynna «JabMMaTenbHaa 1 rpyona «Ipacrayeckas\n" +
                "auleXTpOHHka» IeKTPOHHKa»\n" +
                "TL10 Bopobses TM. TO Bopoéses .M.\n" +
                "4 17,15-17.55 2 rpynna «3anHMarenbHas 1 rpyana «Ipakruyeckas\n" +
                "3EKTPOHHKaY NIEKTPOHHKa»\n" +
                "140 Bopobses CM. I1H1O Bopo6bses C.M.\n" +
                "5 18.00-18.40\n" +
                "6 18.45-19,25\n" +
                "7 19.30-20.10\n" +
                "\n" +
                "Bowe GOP fogs\n" +
                "\n" +
                "\fPacnucanne 3anAnTHH\n" +
                "Jlaboparopua 3D npororanuposanna\n" +
                "TLIO — Kamen JIL.b.\n" +
                "\n" +
                "Ne | Bpema Tloneqebank Bropuuk | Cpeaa Uersepr | TistHnya | Cy6d0ra\n" +
                "1 emena\n" +
                "\n" +
                "1 | 09.00-09.40\n" +
                "\n" +
                "2 | 09.45-10.25 1 rpynna «Ocuoss! 3D 1 rpynna «OcHosai 3D\n" +
                "MOJeIHpoBaHHa> MOZeNpoBaHHs»\n" +
                "TLIO Kamen JIB. TLIO Kamen J.B.\n" +
                "\n" +
                "3 | 10.30-11.10 1 rpynna «OcHossi 3D 1 rpynna «OcnossI 3D\n" +
                "MOJeIMpOBaHHa»> MOfenHpOBaHHa»\n" +
                "TIO Komen J1.b. TUIO Kameu J.B.\n" +
                "\n" +
                "4 | 11.15-11.55\n" +
                "\n" +
                "2 cmena\n" +
                "\n" +
                "1 | 15.00-15.40\n" +
                "\n" +
                "2 | 15.45-16.25 2 rpynna «OcHospl 3D 2 rpynna «OcHoppl 3D\n" +
                "Modes HpoBaHHa» MOJeNHpOBaHHA»\n" +
                "TLIO Komen JLB. TIO Komen JLB.\n" +
                "\n" +
                "3 | 16.30-17.10 2 rpynna «OcHossr 3D 2 rpynna «OcHosst 3D\n" +
                "MOJeNMpOBaHHa» MOenupoBaHHa»\n" +
                "TIO Komen JLB. TIO Komen JIB.\n" +
                "\n" +
                "4 | 17.15-17.55\n" +
                "\n" +
                "5 | 18.00-18.40\n" +
                "\n" +
                "6 | 18.45-19.25\n" +
                "\n" +
                "7 | 19.30-20.10\n" +
                "\n" +
                "8 | 20.15-20.55\n" +
                "\n" +
                "an YOP Leds\n" +
                "\n" +
                "\fPacnucanne 3anaThii\n" +
                "Jla6oparopaa paaHo\n" +
                "TIO — Comonenko B. A.\n" +
                "\n" +
                "Ne | Bpema TloneseibHHk Bropurk | Cpena YWersepr Tlatanua | Cy660Ta\n" +
                "1 cmena\n" +
                "1_ | 09.00-09.40\n" +
                "2 | 09.45-10.25\n" +
                "3 | 10.30-11.10 1 rpynna «Merposorna a | rpynna\n" +
                "paqHonsmepenna» «Merposioraa 4\n" +
                "TIO Cononenxo B.A. payMonsmMepeHHay\n" +
                "TIO Conovenko B.A.\n" +
                "4 | 11.15-11.55 1 rpynna «Metposorna 4 l rpynna\n" +
                "palMon3MepeHHa «MetTposiorua 1\n" +
                "{10 Conouenko B.A. padqMousmepeHHay\n" +
                "[L170 Conovenxo B.A.\n" +
                "- 2emena\n" +
                "1 | 15.00-15.40 2 rpynna «Metponorua u 2 rpynna\n" +
                "pagMousmMepeHHa» «Metponorna 4\n" +
                "MO Cononenxo B.A. panHuou3mMepeHua»\n" +
                "TO Cononenko B.A.\n" +
                "2 | 15.45-16.25 2 rpynna «Merponorus 4 2 rpynna\n" +
                "panHou3smMepenna» «Merponorua 4\n" +
                "THO Cononenko B.A, palMou3sMepeHHs»\n" +
                "TIO Conovexko B.A.\n" +
                "3. | 16.30-17.10\n" +
                "4 | 17.15-17.55\n" +
                "5_| 18.00-18.40\n" +
                "6 | 18.45-19.25\n" +
                "7_| 19.30-20.10\n" +
                "& | 20.15-20.55\n" +
                "\n" +
                "\fPacnucanne 3anATHH\n" +
                "KaGuner no chopke Aponos\n" +
                "ILO — JIucanos B.B.\n" +
                "Ne | Bpems | Tlonegenbnnc | Bropunic L. Cpena | Yerpepr [ Tarawa |\n" +
                "lemena\n" +
                "1 “oe10= 2 rpynna «/[ponbic ayia» ff Pp 2rpynna«/Ipousic nym» 4 |...\n" +
                ", JIucanos B.B. a, Ve. Jlucaion B.B. aff ie\n" +
                "2 . 2 rpynna «JJpoHbi ¢ Hya» ia 2 rpyrina «J[poubi c HYIIA»\n" +
                "OP Aa-10i25 JIncanos B.B. Jlucanos B.B.\n" +
                "3 2 rpyrnna «SCHOOLA DRONE» 2 rpynna «SCHOOLA DRONE»\n" +
                "10.35-11.10\n" +
                "Jincanos B.B. JlucaHos B.B.\n" +
                "4 11.15-11.85 2 rpynna «SCHOOLA DRONE» 2 rpynna «SCHOOLA DRONE»\n" +
                ", i JImcanoe B.B. Jlucanos B.B.\n" +
                "5 12,00-12.40\n" +
                "6 12.45-13.25\n" +
                "2 cmena\n" +
                "1 15.00-15.40\n" +
                "2 | 15.45-16.25\n" +
                "3 16.30-17.10 T rpynna «JIpoubi c Hy» 1 rpynna «Jiponsi c nya»\n" +
                ". ‘ JIucauos B.B. JIvcaHos B.B.\n" +
                "4 17.15-17.55 1 rpynna «/[pousi c Hyna» 1 rpynna «JIponsi c Hya»\n" +
                "i i JIucanos B.B. Jineatton B.B.\n" +
                "« 1 rpynna «SCHOOLA DRONE» 1 rpynna «SCHOOLA DRONE»\n" +
                "$ 18.00-18.40\n" +
                "JIucanoe B.B. Jiucanos B.B.\n" +
                "6 18.45-19.25 1 rpynna «SCHOOLA DRONE» 1 rpynua «SCHOOLA DRONE»\n" +
                "‘ , JIucanos B.B. JIucanos B.B.\n" +
                "7) 19.30-20.10 1 rpynna «becnnnoTuas apnaunsa» 1 rpynua «Becnujoreas annals»\n" +
                "‘ ‘ Jiucanos B.B, Jlucauos B.B.\n" +
                "1 rpynna «BecnmaoTuaa asvauuan ] rpynna «Becnnnoruas agnanna»\n" +
                "S || 26:b:2055 JIvcanos B.B. JIucanos B.B.\n" +
                "4 :\n" +
                "5 eur J (2 4 ef\n" +
                "Bee 2 o- e ie\n" +
                "\n" +
                "\fKaGuner SOFT nporpaMMuporannn\n" +
                "ILO — Tadéacoz K.JI.\n" +
                "\n" +
                "Paconcanne 3aHnaTHii\n" +
                "\n" +
                "Ne | Bpema Tloneaeabnak Bropruk Cpeaa L Wernepr | Tlarnnua Cy66oTa\n" +
                "1 cmena\n" +
                "\n" +
                "1 | 09.00-09.40\n" +
                "\n" +
                "2 09.45-10.25\n" +
                "\n" +
                "3 | 10,30-11.10\n" +
                "\n" +
                "4 11.15-11.55\n" +
                "\n" +
                "5 |_12.00-12.40\n" +
                "\n" +
                "6 | 12.45-13,25\n" +
                "\n" +
                "7 13,30-14,10\n" +
                "\n" +
                "2 cmena\n" +
                "\n" +
                "1 15.00-15.40 2 rpynma «Programming» 2 rpynma «Programming»\n" +
                "M110 Ta66acoe K.JI. (L110 la66acorn K_JI.\n" +
                "\n" +
                "2 | 15.45-16.25 2 rpymna «Programming» 2 rpynna «Programming»\n" +
                "TIO Ta66acor KJ. 10 Ta66acon Kf.\n" +
                "\n" +
                "3 16,30-17.10 3 rpynna «Programming» 3 rpynna «Programming»\n" +
                "TIO Ta66acon K.JI. TIO la66acos K.JI.\n" +
                "\n" +
                "4 | 17.15-17.55 3 rpymna «Programming» 3 rpyrna «Programming»\n" +
                "NO Ta66acon K.J1. TIO Ta66acor K.JI.\n" +
                "\n" +
                "5 | 18.00-18.40\n" +
                "\n" +
                "6 18.45-19.25\n" +
                "\n" +
                "7 | 19.30-20,10\n" +
                "\n" +
                "\fPacnucanHne 3aHRTHH\n" +
                "PoéoroTexHHkn\n" +
                "ILO -Topéas 11.0.\n" +
                "\n" +
                "we»  YTBEP2KJIAIO\n" +
                "\n" +
                "TKK (leopen. mko.1bHnKkos»\n" +
                "\n" +
                "Jtupergop\n" +
                "\n" +
                "AGn IbMaKHNOBa 712K.\n" +
                "\n" +
                "Se\n" +
                "No | Bpema Tlonegentunk Bropnak Cpena Yersepr Iistanua Cy66bora\n" +
                "1 cmena\n" +
                "1 | 09.00-09.40 08.30-9.10 08.30-9.10 10.00-10.40\n" +
                "3 rpynna «ROBO- 3 rpynma «ROBO- | | rpynna «ROBOTICS»\n" +
                "spike» ILO Top6au spike» ITO TO Pop6ax J.0.\n" +
                "J1.0. Top6au JO.\n" +
                "2 | 09.45-10.25 09.15-09.55 09.15-09.55 10.45-11.25\n" +
                "3 rpynma «ROBO- 3 rpynna «ROBO- | | rpynma «ROBOTICS»\n" +
                "spike» MIO Top6ayu spike» ITO MO Topbay JT.0,\n" +
                "1.0. Top6au JLO.\n" +
                "3 | 10.30-11.10 11.30-12.10\n" +
                "2 rpynna «ROBO-spike»\n" +
                "TAO Pop6ay JI.0.\n" +
                "4 | 11.15-11.55 12.15-12.55\n" +
                "2 rpynna «ROBO-spike»\n" +
                "TO Top6ay 1.0.\n" +
                "2 cmeHa\n" +
                "3 | 16.30-17,10 17.00-17.40\n" +
                "1 rpynna «ROBOTICS»,\n" +
                "TL{O Popbax JO.\n" +
                "4 | 17.15-17.55 17.45-18.25\n" +
                "1 rpynna «ROBOTICS»\n" +
                "ITO Popbax 7.0.\n" +
                "§ | 18.00-18.40 18.30-19.10\n" +
                "2 rpynria «ROBO-spike»\n" +
                "TO Pop6ay 71.0.\n" +
                "6 | 18.45-19.25 19.15-19.55\n" +
                "2 rpynna «ROBO-spike»\n" +
                "| TIO Top6as JO.\n" +
                "oe\n" +
                "Sau SP JO cd\n" +
                "\n" +
                "\fPacntucanne 3aHATHH\n" +
                "«<Onbiii n306peTaTesb»\n" +
                "10 — Tyanrenos T.K.\n" +
                "\n" +
                "Ne | Bpema | TonenembHHk Bropank Cpeaa | “erpepr TistHnua Cyé6oTa\n" +
                "1 cmena\n" +
                "\n" +
                "l 09.00-09.40\n" +
                "\n" +
                "2 | 09.45-10.25\n" +
                "\n" +
                "3 10.30-11.10\n" +
                "\n" +
                "4 11.15-11.55\n" +
                "\n" +
                "5 12.00-12.40\n" +
                "\n" +
                "6 12.45-13.25\n" +
                "\n" +
                "2 cmeHa\n" +
                "\n" +
                "1 15.00-15.40\n" +
                "\n" +
                "2 15.45-16.25 16,00-16.40 16.00-16,40\n" +
                "1 rpynna «lOnere 1 rpynma «Ouse\n" +
                "uzo6peTarss1a» H306petaTenn»\n" +
                "TO Tymrenos T.K. M10 Tyaurexos T.K.\n" +
                "\n" +
                "5 16.30-17.10 16.45-17.25 16.45-17.25\n" +
                "| rpynna «iOnpte 1 rpynna «lOusie\n" +
                "wao6peTaTenn» w306petarenn»\n" +
                "NO Tysmrevos T.-K. NO Tynmrevtos T.K.\n" +
                "\n" +
                "4 17.15-17.55 17.30-18.10 17,30-18.10\n" +
                "| rpynna «ROBOTICS» 1 rpynna «ROBOTICS»\n" +
                "TO Tynurenos T.K. MAO Tyaurenoe TK.\n" +
                "\n" +
                "5 18.00-18.40 18.15-18.55 18,15-18.55\n" +
                "| rpynna «ROBOTICS» 1 rpynna «ROBOTICS»\n" +
                "HO Tyaurenos T.K. TO Tynmretion T.K.\n" +
                "\n" +
                "6 18.45-19.25 19.00-19.40 19,00-19.40\n" +
                "2 rpymma «ROBOTICS» 2 rpynna «ROBOTICS»\n" +
                "S110 Tynwrenos T.K. TIO Tyaurevon T.K.\n" +
                "\n" +
                "7 19.30-20.10 19.45-20.25 19.45-20.25\n" +
                "2 rpymma «ROBOTICS» 2 rpynna «ROBOTICS»\n" +
                "M110 Tynurevos T.K. MLO Tynnrenos T.K.\n" +
                "\n" +
                "ban OP Fat\n" +
                "\n" +
                "\fPacnucanne 3ansaTuii\n" +
                "Ka6énner SOFT nporpammupopanna\n" +
                "IDO — Mankomes 2K.H.\n" +
                "\n" +
                "Ne | Bpemsa | Monesentunk | Bropruk Cpeaa | Uersepr | Isrunna | Cy66ora\n" +
                "1 emena\n" +
                "1 | 09.00-09.40 1 rpyona «Ocuosst\n" +
                "MporpaMMupowania»\n" +
                "TIO Marxowwes K.H.\n" +
                "2 | 09.45-10.25 1 rpyona «OcHospI\n" +
                "MporpaMMHpoBanHa»\n" +
                "ILO Marxomes K.H.\n" +
                "3 10.30-11,10 | rpynna «I IporpamMupopanue na\n" +
                "Python»\n" +
                "T110 Manxomes KH.\n" +
                "4 | 11.15-11.55 1 rpynna «!porpammnpopanne Ha\n" +
                "Python»\n" +
                "TLIO Maxxonies XK.H.\n" +
                "5 12,00-12.40 2 rpyuna «[lporpamMupopaHie na\n" +
                "Python»\n" +
                "TIO Marxowes K.H.\n" +
                "6 | 12.45-13.25 2 rpynna «lporpammnpopanue Ha\n" +
                "Python»\n" +
                "TIO Manxowes 3K.H.\n" +
                "7 13.30-14,10 2 rpymna «lIporpammuporanne na\n" +
                "Python»\n" +
                "THO Maxxoures 3K.H.\n" +
                "2 emena\n" +
                "1 15.00-15.40\n" +
                "2 15.45-16.25\n" +
                "3. | 16.30-17.10 | rpynna «OcHoss nporpamMupopania»\n" +
                "TIO Manxoines K.H.\n" +
                "4 | 17.15-17.55 ] rpynna «OcHossi nporpamMaponaHHa»\n" +
                "TIO Manxomes KH.\n" +
                "5 | 18.00-18.40 1 rpyona «I porpammMuporanve Ha\n" +
                "Python» :\n" +
                "TIO Manxomes JK.H.\n" +
                "6 | 18.45-19.25 1 rpynna «Iporpammnposanve Ha\n" +
                "Python»\n" +
                "TIO Manxowes K.H.\n" +
                "7 | 19,30-20.10 2 rpynoa «[porpammuposanve Ha\n" +
                "Python»\n" +
                "THO Manxomes KH.\n" +
                "\n" +
                "Jau Y2P By\n" +
                "\n" +
                "\fPacnucanie 3annTHit\n" +
                "\n" +
                "Jla6opaTropua HpOMBINIeHHOrO HHTepHera Beljeii\n" +
                "\n" +
                "TLIO — Wesyenko H.B.\n" +
                "\n" +
                "Cee\n" +
                "\n" +
                "Se hang, ae\n" +
                "at 20T7Es a\n" +
                "\n" +
                "No | Bpema TlonegenbanKk BropHuk | Cpena Uerpepr | Tiarnuua | Cyé6otTa\n" +
                "1 cmena\n" +
                "1 | 09.00-09.40 1 rpynna «KomnprorepHaa 1 rpynna «KomnbiorepHas\n" +
                "MYJIbTHILIMKALlHa» MYJIBTHIVINK aly)\n" +
                "MO Liesyenko H.B. [110 IWesyernxo H.B.\n" +
                "2 | 09.45-10.25 1 rpynna «Komnbiorepnaa 1 rpynna «KommproTepHaa\n" +
                "MYJIBTHIMIHKall Ha» MYJIBTHIWIMKaLH a>\n" +
                "MO UWesyenko H.B. T11O MWiesyenko H.B.\n" +
                "3 | 10.30-11,10\n" +
                "4 | 11.15-11.55\n" +
                "2 cmeHa\n" +
                "1 | 15.00-15,.40 2 rpynna «KomnBHhorepuas 2 rpynna «KommbrorepHan\n" +
                "MYJIBTHIMUINKallHa» MYJBTHIIMKAauHAY\n" +
                "N10 Wesyenxo H.B. T1710 Ulesyenxo H.B.\n" +
                "2 | 15.45-16.25 2 rpyona «Kommpiorepyas 2 rpynna «KomMMmboTepHas\n" +
                "MYJIbTHIMAIH Kau)» MYIBTHIUIMKALAA)\n" +
                "T1LO lesuenko H.B. TIO Uesyenko H.B.\n" +
                "3 | 16.30-17.10\n" +
                "4 | 17.15-17.55\n" +
                "S| 18.00-18.40\n" +
                "6 | 18.45-19.25\n" +
                "7_| 19.30-20.10\n" +
                "8 | 20.15-20.55\n" +
                "\n" +
                "\fPacnncanne 3anATHA\n" +
                "\n" +
                "PoéjoroTexHnka\n" +
                "\n" +
                "TL0 — Kaxumos K.2K.\n" +
                "\n" +
                "Neo Bpema TlonexenbHuk Bropuuk Cpena Ilatnuna | Cy6ébora\n" +
                "1 cmena\n" +
                "1 9:00-9:40 1 rpynma «FOuEIe\n" +
                "n306perareiu»\n" +
                "TIO Kaxumoer KOK.\n" +
                "2 | 9:45-10:25 1 rpynna «FOuste\n" +
                "H306peTaTenH»\n" +
                "TIO Kaxumos KK.\n" +
                "Ee] 10:30-11:10 | rpynna\n" +
                "«ROBOTICS»\n" +
                "T1JO Kakumos KK.\n" +
                "4 11:15-11:55 | rpynma\n" +
                "«ROBOTICS»\n" +
                "TIO Kaxumos KK\n" +
                "5 15:00-15:40\n" +
                "6 15:45-16:25\n" +
                "7 16:30-17:10\n" +
                "8 18:00-18:40 1 rpynna «lOupte\n" +
                "H306peraresH»\n" +
                "[IO Kaxumor KK.\n" +
                "9 18:45-19:25 1 rpynna «bOupie\n" +
                "wso6peraTrenu»\n" +
                "TIO Kaxumos K.2K.\n" +
                "10 | 19:30-20:10 1 rpynna\n" +
                "«ROBOTICS»\n" +
                "ITO Kaxumos KOK\n" +
                "11) 20:15-20:55 1 rpynna\n" +
                "«ROBOTICS»\n" +
                "\n" +
                "THO Kaxumos KK\n" +
                "\n" +
                "Saun GSP BLA\n" +
                "\n" +
                "\fPacnncanne 3ansaTHH\n" +
                "Barzapamaulbi\n" +
                "ILO — KaGayskasikos DK.M.\n" +
                "\n" +
                "Ne | Bpema | Toneqeabnnk Bropuuk Cpena | Gersepr TstHnua CydooTa\n" +
                "lcmena\n" +
                "1 09.00-09.40 1 rpynna «BaraaplamMaluLD 1 rpynna «Baraapmamalibp>\n" +
                "[ILO Ka6nynxanpiros 2K.M. TIO KaSaynxanprxos 9K.M.\n" +
                "2 | 09.45-10.25 1 rpynna «Barjapramanin 1 rpynna «Bargapnamalliby\n" +
                "TIO KaGaysxanpKos 3K.M. T11lO KaGay nxanpixos 0K.M.\n" +
                "3 10.30-11.10 | 2 rpynna «Bbarmapaamauibn» 2 rpynma «bartapqamallipl»\n" +
                "TIO Kadayaramixos 4M. TO Kaéayananixos 2K.M.\n" +
                "4 11.15-11.55 | 2 rpynoa «bargapnamautpl 2 rpynna «bargapsiamalibh»\n" +
                "TIO Ka6aynxanprxop 2K.M. M110 Kaéaynnantixos 1K.M.\n" +
                "5 12.00-12.40\n" +
                "6 12.45-13.25\n" +
                "2 cmena\n" +
                "I 15,00-15.40\n" +
                "2 15.45-16.25\n" +
                "3 16.30-17.10\n" +
                "4 17,15-17.55\n" +
                "5 18.00-18.40\n" +
                "6 18.45-19.25\n" +
                "f 19.30-20.10\n" +
                "\n" +
                "Gome GOP\n" +
                "\n" +
                "\fCa6ak kecrteci\n" +
                "«AcBLI Mypa» JomMO6nipa yiiipmeci\n" +
                "\n" +
                "TLJ[O: Kanatos B.C.,\n" +
                "Ni] Bpems Tlonegemnnk Bropunk Cpeaa Yersepr Tiara Cy6Gora Bockpecenne\n" +
                "lcmena\n" +
                ": ae «ACBL Mypa» «ACBL Mypa» 09.00-09.40\n" +
                "I rpynma Lrpynna\n" +
                "all Ba «AcBIN Mypa» «ACBL Mypa» 09.45-10.25\n" +
                ": I rpynna Lrpynna\n" +
                "dl eas «AcBLI Mypa» «Acbul Mypa» 10.30-11.10\n" +
                "j Il_rpynna IL rpynna\n" +
                "Pil ee «ACBUI Mypa» «ACBLI Mypay 11.15-11.55\n" +
                ": Il rpynna IL rpynna\n" +
                "; ae «AcbLT M¥pa» «AcBL Mypa» 14.00-14.40\n" +
                "; TL rpynna I) rpynna\n" +
                "2) eae «AcbuI Mypa» «AcBi Mypa» 14.45-15.25\n" +
                "ia Il rpynna Ill rpynna\n" +
                "abe «ACBLI Mypa» «ACEI Mypa» 15.30-16.10\n" +
                "; IV rpynna TV rpynna\n" +
                "aa «ACEUI Mypa «ACBL M¥pa» 16.15-16.55\n" +
                ": IV rpynna IV rpynna\n" +
                "5) 18.00- 17.00-17.40\n" +
                "18.40\n" +
                "6 |) 18.45- 17.45-18.25\n" +
                "19.25\n" +
                "\n" +
                "Kepkem-3cTeTakaJIbik OaFbiTTbInbIn MeHrepymici oS Rosita 7” Tycynog AK.\n" +
                "\n" +
                "\fPacnnecanne 3annTai\n" +
                "Kpyxok «Tonaraii» Gananap TeaTphi\n" +
                "\n" +
                "TIO: Tycynos A.K.,\n" +
                "Epemsa Nonegem nuk Bropant Cpeza Uernepr Tarnnana Cy66ora\n" +
                "1 emena\n" +
                "9.00- 09.00-\n" +
                "9.40 09.40\n" +
                "09.45- 09 45-\n" +
                "10,25 10.25\n" +
                "10.30- 10,30-\n" +
                "11.10 11.10\n" +
                "11.15- 1115+\n" +
                "11.55 11.55\n" +
                "2 cmena\n" +
                "15.00- 14,00-\n" +
                "15.40 14.40\n" +
                "15,45- 14.45-\n" +
                "16.25 15.25\n" +
                "16,30- 1 ton «Tenaraip 1 ton «Toaaraity Ganaaap TeaTpht 15,30-\n" +
                "17,10 Ganaap TeaTpet Tycynios AK. 16.10\n" +
                "Tyeynos A.K.\n" +
                "17,15- 1 ron «Tonaraiin | ron «Tosaraim Gananap Teatpe 16.15-\n" +
                "17.55 Ganansp Teatper Tycymos A.K 16,55\n" +
                "Tyeynos AK.\n" +
                "18.00- 2 ron «Tonaraiby 2 ton «Tonaraim Gananap TeaTpLt 17.00-\n" +
                "18.40 Gatanap TeaTpEr Tycynos A.K 17,40\n" +
                "Tycynos AK.\n" +
                "18.45- 2 ton «Tonaraiin 2 ron «Tosaraim Gananap TeaTphi 17.45-\n" +
                "19.25 Gatamap TeaTper Tycynos AK 18,25\n" +
                "Tycynos AK.\n" +
                "\n" +
                "Kepkem-3cTeTHkaJILik 6aFrbiTTbMibin Menrepymici ae Tycynos A.K.\n" +
                "\n" +
                "Sace Yb By\n" +
                "\n" +
                "\f«Tonaraim Oananap Teatppt\n" +
                "KBBM: Toiiéasapos A.M.,\n" +
                "\n" +
                "Ne] Bpema TMlonenesHHk Bropuux Cpeaa Yersepr Tarra Cyibora Bockpecente\n" +
                "lemena\n" +
                "1 | 9.00- 1 ton «Tonaraim Gananap 09.00-09.40 1 ron «Tosaraii»Gananap\n" +
                "9.40 TeAaTpEl TeaTpht\n" +
                "ToiiGasapos j!|,.M_ Toiiarapos J..M.\n" +
                "2 | 09.45- L ton «Tonaraim Gananap 09.45-10.25 1 ron «Tonarai»6ananap\n" +
                "10.25 TeaTpal TeaTphl\n" +
                "Toit6asapon J1.M_ Toa#Gaszapoa JM.\n" +
                "3 | 10.30- 10.30-11.10 | 2 Ton «Tonaraii» Gananap\n" +
                "11.10 TeaTppt\n" +
                "TofGazapos JM.\n" +
                "4 | 1L15- 11.15-11.55 2 Ton «Tosaraii» Garanap\n" +
                "11.55 TeaTpbi\n" +
                "TotGasapou JLM.\n" +
                "2 cmeHa\n" +
                "1 | 15.00- 2 ron «Tonaraitm Gananap 14.00-14,.40\n" +
                "15.40 reaTpEt\n" +
                "Tofi6asapon JI.M.\n" +
                "2 | 15.45- 2 ron «Tonarai Gananap 14.45-15.25 | 3 to1 «Tosraraii» Gananap\n" +
                "16.25 TeaT pei TeaTphi\n" +
                "Tosi6asapon J].M. ToiiGasapos J.-M.\n" +
                "3 | 16.30- 15.30-16.10 | 3 ton «Tonarait» Gananap\n" +
                "17.10 TeaTpb!\n" +
                "Toti6azapon /1.M.\n" +
                "4 | 17.15- 16.15-16.55\n" +
                "17.55\n" +
                "5 | 18,00- 3 ron «Tonaraim Garanap 17.00-17.40 3 ton «Toniraim fananap\n" +
                "18.40 TeaTpbt TeaTpEl\n" +
                "TofiGasapon J1.M_. ToliSazapos JLM..\n" +
                "6 | 1845- 3 ron «Tonaratm Gananap 17,45-18.25 3 ton «Tonaraim Gananap\n" +
                "19.25 reatpbt TeaTpsI\n" +
                "ToRéasapon 1M. Toithasapos JL.M.\n" +
                "\n" +
                "Kepkem-3creTHkaJinik 6aFrbiTTLinbin MeHrepymici ae Tycynos A.K.\n" +
                "\n" +
                "Secu OP Hf)\n" +
                "\n" +
                "\fCa6éak Kecteci\n" +
                "«Ma6err» JomOonipa yitipmeci\n" +
                "KBBM: Ocnanosa 3.C.,\n" +
                "\n" +
                "Me | Yaxeer Jiyitcenti Ceiicenbi Capcenbi | Beiicen6i Kyma Cen6i\n" +
                "1 cmena\n" +
                "1 | 9.00- 1 rom «lWa6srr» 1 tom «la6piT\n" +
                "9.48 Ocnanosa 3.C. Ocnanona 3.C.\n" +
                "2 | 09.45- 1 ton «la6ziT» 1 ron «lLa6niT»\n" +
                "10.25 Ocnanoga 3.C. Ocnanora 3.C,\n" +
                "3 | 10.30- 2 ton «lLa6srT 2 ton «aber»\n" +
                "11.10 Ocnanoga 3.C. Ocnanoga 3.C,\n" +
                "4 | 11.15- 2 ton «lLla6niT» 2 ton «adit»\n" +
                "EDS OcnaHoga 3.C. Ocnauosza 3.C.\n" +
                "5 | 12.00-\n" +
                "12.50\n" +
                "6 12,55-\n" +
                "13.35\n" +
                "7 | 13.40- 3 ton «lLa6niT» 3 Ton «a6siT»\n" +
                "14.20 Ocnanosa 3.C. Ocnanoga 3.C.\n" +
                "8 | 14.25- 3 ron «abet» 3 Ton «abst»\n" +
                "15.05 Ocnanopa 3.C. OcnaHoga 3.C.\n" +
                "9 | 15.10- 4 ton «LLa6nir»\n" +
                "15.50 Ocnanora 3.C. 4 tom «ILla6piT»\n" +
                "Ocnanosa 3.C.\n" +
                "10 | 15.55-\n" +
                "1630 4 a mena “ 4 ton «La6pit»\n" +
                "a OcnanHosa 3.C.\n" +
                "\n" +
                "Kepkem-3creTHkaJibiK OaFbITTbIHbIn MeHrepyliici ag Tycynos A.K.\n" +
                "\n" +
                "bau JAP\n" +
                "\n" +
                "\fBokaabuoii cryium «Belcanto»\n" +
                "\n" +
                "KBBM: Tormoric6ex JK.,\n" +
                "Ne) Bpema Tonegeanninc Bropumkc Cpeaa Yersepr TistHaua Cy6Gorta Bocxpecenbe\n" +
                "1 emena\n" +
                "1] 9.00- . 09.00-09.40\n" +
                "9.40\n" +
                "2 | 09.45- : 09.45-10.25\n" +
                "10.25\n" +
                "3 | 1030- ~10.30-11.10 | ,\n" +
                "1.10\n" +
                "4] iis - 7 OS - a “115-1155 [\n" +
                "11.55 - _\n" +
                "2 emeHa\n" +
                "1 | foo. ! rpyrima Bowantnan | | 7 - OO | -_ ] 1400-1440 14 rpynma Boraspnan cTyaHa\n" +
                "15,40 eTyana «Belcanto» «Belcanto»\n" +
                "Aumpic6aii E.H. Asnpic6ali E.H.\n" +
                "Pa | 1845. 1 rpynna Bokanpnaa UL 2 - 14,45-15.25 | 1 rpynna Bokanbnan cTyaHa\n" +
                "Tes etyana «Belcanto» «Belcanto»\n" +
                "Ammpic6aii E.H. Agmpic6ait E.H.\n" +
                "7) 1630-3 rpynna Bokasbnan _ 15.30-16.10 | 2 rpynna Bokanbnast cryana\n" +
                "1E10 cTyaHa «Belcanto» «Beleanto»\n" +
                "— |__| Annprcbait E.H. oe 7 ee a Annpic6aii EH, _\n" +
                "4 | 1715- | 2 ppynna Bokanbnaa 16.15-16.55 | 2 rpynna Bokanbyan cryana\n" +
                "Tea eTyaHa «Belcanto» «Belcanto»\n" +
                "=e Aompicoait EH. _ | _ _ Anmpic6aii E.H.\n" +
                "5 | 1800- | 3 rpynna Bokatpbuan 17.00-17.40 | 3 rpynna Bokaqbuaa eryann\n" +
                "1840 | erynann «Belcanto» «Beleanto»\n" +
                "Aumpic6ait E.H. AnnsicOaii E.H.\n" +
                "6 | 18.45- 3 rpyuna Bokatbuan 17.45-18.25 | 3 rpynna Boxaabuaa cTyana\n" +
                "28 cTyaHa «Belcanto» «Belcanto»\n" +
                "Asnsic6aii E.H. AsnpicGali E.H.\n" +
                "Neprem-seToTukaJbik GarbiTThIHbIn MeHrepyiilici Tycynos A.K,\n" +
                "\n" +
                "te {OP Lory\n" +
                "\n" +
                "\fCaG6ak kecteci\n" +
                "«banbyiak» jomMObIpa yiipmeci\n" +
                "KBBM: Kyanabn OUK.,\n" +
                "\n" +
                "No | Bpemsa Tlonenenbank BropHuk Cpena Uersepr Tisraaua Cy66ora\n" +
                "1 emena\n" +
                "1_| 9.00-9.40\n" +
                "2 | 09.45-\n" +
                "10.25\n" +
                "3 | 10.30-\n" +
                "11.10\n" +
                "2 cmena\n" +
                "1 | 15.00-\n" +
                "15.40\n" +
                "2 | 15.45-\n" +
                "16.25\n" +
                "3 | 16.30-\n" +
                "17.10\n" +
                "4 | 17.15-\n" +
                "17.55\n" +
                "5 | 18.00-\n" +
                "18.40\n" +
                "6 | 18.45- 1 ton «ban6ynak» omOpipa 1 ton «ban6ynak» TOoM6bIpa\n" +
                "19.25 yiiipmeci yitipmeci\n" +
                "Kyanapik OK. KyaHanik OOK.\n" +
                "7 | 19.30- 1 ton «ban6ynak» Tom6nipa 1 ton «ban6ynak» om6pipa\n" +
                "20.10 yitipmeci yitipmeci\n" +
                "Kyananik OOK. Kyarapik OK.\n" +
                "8 | 20.15-\n" +
                "20.55\n" +
                "\n" +
                "Kepkem-3cTeTHKaJIbIK O6aFbITTbIHbIn MeHrepymlici Za f* ‘yeynos A.K.\n" +
                "\n" +
                "Sau SP erty\n" +
                "\n" +
                "\fPacnneauue 3ans TH\n" +
                "\n" +
                "KpyxoxK Xopeorpauyecknii ancam6n«Camra»\n" +
                "\n" +
                "TO: Tememko FO.H.,\n" +
                "Ne| Bpema Toneges nik Bropanx Cpeaa Uernepr Tatanua\n" +
                "a 7 ae T emena\n" +
                "1/900 | Sa — | 3 rpyrna Xopeorpaduseckuit ancam6nn “Camra | - 7 3 rpynna Xopeorpaduyecxuii ancam6ne “Camra\n" +
                "940 Hlemenrxo 10.H. Alemeuko FO.H,\n" +
                "2 | 0945. = —- 3 rpynna Xopeorpadumecknit aneaméas, “Canra se 3 rpynina Xopcorpatuveckuii ancam6ap “Camra\n" +
                "10.25 Hlemenrko 10.H. Zememrko 1O.H,\n" +
                "“3'| 10.30- ee 7 —_ a _\n" +
                "eat)\n" +
                "4 | 11.15- a -\n" +
                "1155 _ — — _—\" ae | _\n" +
                "2 cmena\n" +
                "1 | 15,00-\n" +
                "15.40\n" +
                "2 | 15.45- 2 rpynna Xopeorpadpuyeckuii ancam6ab “Camra 2 rpynua Xopeorpaduueckuii aucamOnp “Camra\n" +
                "16.25 Alemeuxo 10.H. Temeuko [0.H.\n" +
                "3 | 16.30- 1 rpynna 2 rpynna XopeorpaibHyecknit ancamOab “Cara 2 rpynna Xopeorpapusecknii aticamOnb “Camra\n" +
                "17.10 Mopeorpadusecknit Tememno 10H. Jlemeurko 10.H.\n" +
                "ancaM6nb “Camra\n" +
                "Jlememxo 10.H.\n" +
                "4) 17.15- 1 rpynna i rpynna Xopeorpaduyecknii ancamOnb “Camra\n" +
                "17.55 Xopeorpadpwyeckuit Aememxo 10.H.\n" +
                "ancamGmp “Camra\n" +
                "Hemeuixo 1O0.H,\n" +
                "5 | 18.00- l rpynna XopeorpaduyeckHit ancam6ap “Cana\n" +
                "18.40 Tememxo t0.H.\n" +
                "6 | 1845-\n" +
                "19.25\n" +
                "\n" +
                "Kopkem-3¢TeTukasibik OarbiTTBInbily MeHrepyulici\n" +
                "\n" +
                "Tycynos A.K.\n" +
                "\n" +
                "\fCa6ak kecTeci\n" +
                "«a6erTt fomO6nipa yitipmeci\n" +
                "\n" +
                "KBBM: Koagachaena A.C.,\n" +
                "Ne | Yaxerr Ceiicenbi Capcen6i | Beitcen6i oKyma Cenbi\n" +
                "1 cmena\n" +
                "1 | 9.00- 1 ron «ban6ynax» 1 rom «ban6ynak»\n" +
                "9.40 AomObipa yitipmeci gomOnipa yHipmeci\n" +
                "WKonmac6aeza A.C ?Komgac6aera A.C\n" +
                "2 | 09.45- 1 ron «ban6ynaK» 1 ron «ban6ynaK»\n" +
                "10:25 HomOnipa yitipmeci somObipa yilipmeci\n" +
                "dKongacbaena A.C Konnacbaepa A.C\n" +
                "3 | 10.30- 2 Ton «Banbysak» 2 Ton «Banbysak»\n" +
                "Llp nomOnipa yitipmeci nomOnipa yilipmeci\n" +
                "2Kongachaewa A.C )Konjacbaepa A.C\n" +
                "4 | 11.15- 2 Ton «ban6ynaxy 2 Ton «ban6yaK»\n" +
                "MLSS nomOsipa yiipmeci nomOpipa yitipmeci\n" +
                "2Kongacbaepa A.C Wonnac6aena A.C\n" +
                "5 | 12.00- 3 Ton «Ban6ys1ax» 3 Ton «ban6ynaK»\n" +
                "1210 omO6pipa yilipmeci nomOeipa yitipmeci\n" +
                "Kongacbaepa A.C MKonmacbaena A.C\n" +
                "6 | 12.55- 3 Ton «ban6yaK 3 Ton «BanGynan\n" +
                "13.35 Hom6zrpa yitipmeci HomObipa yHipmeci\n" +
                "Monnacbaepa A.C Kongacbaepa A.C\n" +
                "7 | 13.40- 4 ron «Banéynax» 4 ron «Ban6ynaK»\n" +
                "14.20 RomGpipa yitipmeci HomOsipa yitipmeci\n" +
                "Kompacbaepa A.C Mompacbaera A.C\n" +
                "8 | 14.25- 4 Ton «ban6y1ak» 4 Ton «Bar6ynak»\n" +
                "Ist aomObipa yitipmeci nomGzrpa yilipmeci\n" +
                "Kongacbaewa A.C MKonnac6aesa A.C\n" +
                "9 | 15.10-\n" +
                "15,50\n" +
                "10 | 15.55-\n" +
                "16,30\n" +
                "\n" +
                "Sau GBP fy\n" +
                "\n" +
                "Kepkem-screTHkasbik GaFbITTbInbin MeHrepyiici DoF Tycynos ALK.\n" +
                "\n" +
                "\fPacntncanne 3annTHii\n" +
                "Kpyxok «KpeaTHBioe pHcoBaHHe\n" +
                "IO: Hsannmesa P.B.,\n" +
                "\n" +
                "“| Bpem NoveqetbHuk Bropauk Cpeaa Yersepr Tlarnaua Cy6toTa\n" +
                "il\n" +
                "l emena\n" +
                "1 | 09.45- 09,45- l rpynna «KpeaTHenoe paconanne»\n" +
                "10,25 10.25 Msannuesa P.B.\n" +
                "2| 10.30- L rpynna «KpeaTusnoe 10.30-\n" +
                "11.10 pucopanne» 11.10 1 rpyrma «Kpeatmpnoe pacosanien\n" +
                "Meanuurena P.B. Haanmmena PB.\n" +
                "3) 1L.15- 1 rpynna «KpeaTupnoe 11.15-\n" +
                "11.55 pHcoBaHHe» 11,55 2 rpynna «KpeaTuanoe pucosanne»\n" +
                "Maannuyesa P.B. Hsarmmesa P.B.\n" +
                "4) 12.00- 12.00- 2 rpynna «KpeaTHBHoe prcopanne»\n" +
                "12.40 Heanuuiecsa P.B.\n" +
                "2 cmeHa 12.40\n" +
                "1) 15.45- 15.45-\n" +
                "16.25 16.25\n" +
                "2 | 16.30- 2 rpynna «KpeatHeroe 16.30-\n" +
                "17.10 il ia 17.10\n" +
                "Meanumena P.B.\n" +
                "3 | 17.15- 2 rpytina «KpeaTusnoe 17.15-\n" +
                "17.55 pHcopanHe» 17,55\n" +
                "Msanninera P.B.\n" +
                "4 | 18.00- 18.00-\n" +
                "18.40 18.40\n" +
                "5) 18.45- 18.45-\n" +
                "19.25 19.25\n" +
                "6 | 19.30- 19.30-\n" +
                "20.10 20.10\n" +
                "\n" +
                "Kopkem-ocTeTHKadbik GaFbiTTbIHbIn MeHrepyutici ol _f  Tyeynos ALK.\n" +
                "\n" +
                "Faun WYP\n" +
                "\n" +
                "\fbed\n" +
                "\n" +
                "“To sgenbHAk\n" +
                "\n" +
                "Braj ine\n" +
                "\n" +
                "Pacnucanne 3annTHit\n" +
                "\n" +
                "Kpyxok rpynna «B kpyry Apy3eii»\n" +
                "TIO -- Akmaram6eTor A.E.,\n" +
                "\n" +
                "Cpene\n" +
                "\n" +
                "i\n" +
                "\n" +
                "S Tpy Mia Kp KOR Pitrappl «B\n" +
                "Kpyry» apyseH\n" +
                "2 ypyina Kpyakok Mitrappr «B\n" +
                "\n" +
                "Kpycy» apy3el\n" +
                "\n" +
                "| epynna Kpyxox rerapsr\n" +
                "«B kpyry» apyseti\n" +
                "\n" +
                "1 cmena\n" +
                "\n" +
                "a\n" +
                "\n" +
                "|\n" +
                "|\n" +
                "|\n" +
                "\n" +
                "2emena\n" +
                "\n" +
                "Uerneps\n" +
                "\n" +
                "| 3 rpyra kpyxok LaTapel «B kpyry»\n" +
                "\n" +
                "ApysH\n" +
                "3 rpytiita xpyaxox rHTapbl «B kpyry»\n" +
                "Apy3eti\n" +
                "\n" +
                "Tarnnuya\n" +
                "\n" +
                "1 rpynna kpyxxoK rurappl «B Kpyry»\n" +
                "Apy3ei\n" +
                "\n" +
                "| rpyuna KpykoOK raTapbt\n" +
                "«B kpyry» apy3eH\n" +
                "\n" +
                "1 rpynna kpyaox rutapei «B kpyry»\n" +
                "apy3el\n" +
                "\n" +
                "2 rpynna kpyxKox raTapbi\n" +
                "«B Kpyry» apy3eH\n" +
                "\n" +
                "2 rpynina Kpyaok Parapel «B Kpyry»\n" +
                "Apy3eHt\n" +
                "\n" +
                "2 rpynna KpyxXoK rutappl\n" +
                "«B Kpyry» apyzeli\n" +
                "\n" +
                "2 rpyniiia Kpy2kok rutapel «B Kpyry»\n" +
                "apy3eH\n" +
                "\n" +
                "=a\n" +
                "Saseqywninit xy fowKecTReEHHO-3ICTeTHY4eCKOrO HallpaBlleHHA D2 Tycynos\n" +
                "\n" +
                "\fPacnnecanne 3anaTHii\n" +
                "Kanner BokaJibHol cryaHH «Belcanto»\n" +
                "ILO: Canumopa CT.\n" +
                "\n" +
                "Ne | Epes Moneaennak Bropunt Cpeaa Uersepr Tariana\n" +
                "1 emena\n" +
                "1 9.00-\n" +
                "9.40\n" +
                "2 | 0945-\n" +
                "10.25\n" +
                "3 10.30-\n" +
                "11.10\n" +
                "4 1L.15-\n" +
                "L155\n" +
                "5 12.00- I rpynna Bokanbuas ctyaus 1 rpynna Boxassxaa cryana «Belcanto» 5 rpynna Bokanbaa cryaua «Belcanto»\n" +
                "12.40 «Belcanto» Canumosa CT. Canumoga CT,\n" +
                "Canumosa CT.\n" +
                "6 12.45- | rpynna BokabHaa ctyaHa ] rpynna BoxanbHas cryaua «Belcantoy 5 rpynna Boxarbuaa ctyaua «Belcantom\n" +
                "13.25 «Belcanto» Canumosa CY. Canumosa CT.\n" +
                "Canumosa CT.\n" +
                "2 cmena\n" +
                "1 13.30- 2 rpynna Boxantnas cryana 2 rpynna Boxatenas cryaua «Belcanto»\n" +
                "14.10 «Belcanto» Canmmoga CLT.\n" +
                "Canumosa CT.\n" +
                "2 | 14.15- 2 rpynna BokanbHas cry qua 2 rpynina Bokatbyaa cTyaua «Belcanto»\n" +
                "14.55 «Belcanto» Canumosa CT.\n" +
                "Canumosa CT,\n" +
                "3 16,00- 3 rpynna Bokaneuas cryaus 3 rpynna Boxansas ctyana «Belcanto»\n" +
                "16.40 «Belcanto» Canumona CI.\n" +
                "Canumona CT,\n" +
                "4 | 16.45- 3 rpynna Bokatbuas ctyana 3 rpynna Bokansuyas cryqua «Belcanto»\n" +
                "17.25 «Belcanto» Camumosa CT.\n" +
                "Canumona CI.\n" +
                "3 | 17.30- 4 rpynna Bokansuas cryaua 4 rpynna Bokansyaa cryaua «Belcantoy\n" +
                "18.10 «Belcanto» 5 rpynna BoxanbHaa ctyqua «Béleanto» Canumopa CI.\n" +
                "Canumogza CI. Canumosa CT.\n" +
                "6 |) 18,15- 4 rpynna Boxansyaa crys 5 rpynna Boxanbuaa cryana «Belcanto»\n" +
                "18,55\n" +
                "\n" +
                "«Belcanto»\n" +
                "Canumona CT,\n" +
                "\n" +
                "Canumosa C.T\n" +
                "\n" +
                "4 rpynna Bokanbuaa crygua «Belcanto»\n" +
                "Canumona CT.\n" +
                "\n" +
                "3aBeqylounil xyf0xKeCTBeCHHO-3cTeTH4ECKOTO Hall paBseHHA i. Tycynos A.K.\n" +
                "\n" +
                "San Wor\n" +
                "\n" +
                "\fPacnncanue 3ansaTHi\n" +
                "Kpyxox «Tonaraii»6ananap Tearpst (pyc)\n" +
                "\n" +
                "ILO: Kankaber H.A.,\n" +
                "\n" +
                "Bpema\n" +
                "\n" +
                "Tlovegenbuwk\n" +
                "\n" +
                "Bropunk\n" +
                "\n" +
                "Cpeaa\n" +
                "\n" +
                "Yerpepr\n" +
                "\n" +
                "TiatHuua\n" +
                "\n" +
                "cy66oTa\n" +
                "\n" +
                "1 cmena\n" +
                "\n" +
                "9.00-9.40\n" +
                "\n" +
                "09.45-\n" +
                "10,25\n" +
                "\n" +
                "10.30-\n" +
                "11,10\n" +
                "\n" +
                "11.15-\n" +
                "11.55\n" +
                "\n" +
                "12.00-\n" +
                "12.40\n" +
                "\n" +
                "12.45-\n" +
                "13.25\n" +
                "\n" +
                "15.00-\n" +
                "15.40\n" +
                "\n" +
                "15.45-\n" +
                "16.25\n" +
                "\n" +
                "16.30-\n" +
                "17.10\n" +
                "\n" +
                "| ton «Tonarai»>Gananap\n" +
                "TeaTpbi (pyc)\n" +
                "Kanka6ek H.A\n" +
                "\n" +
                "1 ton «Tonaraii»banaaap\n" +
                "Teatpbt (pyc)\n" +
                "Kanxa6ex H.A\n" +
                "\n" +
                "1 ton «Toaaraii»bananap TeaTppl\n" +
                "\n" +
                "(pyc)\n" +
                "KanxaGex H.A\n" +
                "\n" +
                "17.15-\n" +
                "17.55\n" +
                "\n" +
                "1 ton «Toaarai>banarap\n" +
                "\n" +
                "TeaTpol (pyc)\n" +
                "Kanxadex H.A\n" +
                "\n" +
                "| ron «ToaaraiimGananap\n" +
                "Teatppi (pyc)\n" +
                "Kanxafiex HA\n" +
                "\n" +
                "1 ton «Totaraii»banarap TeaTpe!\n" +
                "\n" +
                "(pye)\n" +
                "Kanxa6cx H.A\n" +
                "\n" +
                "18:00-\n" +
                "18:40\n" +
                "\n" +
                "18:45-\n" +
                "19:25\n" +
                "\n" +
                "Japenyoumii xy qoKeCTBEHHO-3cTeTH4ECKOrO HalpaBeHnAnt Pra Tycynops A.K.\n" +
                "\n" +
                "Sau YR ba\n" +
                "\n" +
                "\fPacnucanue 3aHATHi\n" +
                "Ka6unet Bokanbnoi crygun «Belcanto»\n" +
                "\n" +
                "ILO: Tapnijona M.H.,\n" +
                "Bpema TlonenesbHnk Bropaar Cpeaa Uersepr Tatowua\n" +
                "Tesena\n" +
                "\n" +
                "9.00-\n" +
                "9,40\n" +
                "09.45-\n" +
                "10.25\n" +
                "10.30- | rpynna Boxasibyas cryaHa 1 rpynna Boxansuas cryana «Belcanto»\n" +
                "11.10 «Belcanto» Tapetoona MU.\n" +
                "\n" +
                "Japtinosa M.H.\n" +
                "H115- | rpynna Bokanbuan cryqua 1 rpynna Bokanbuas ctyaia «Belcanto»\n" +
                "11.55 «Belcanto» Japeropa MH,\n" +
                "\n" +
                "Jassiaosa MH.\n" +
                "12.00- 2 rpynna Boxanbyaa cryua 2 rpynna BokansHaa crygua «Belcanto»\n" +
                "12.40 «Beleanto» Tassujosa MH. +\n" +
                "\n" +
                "Tasbizona M.H.\n" +
                "12.45- 2 rpynna Bokaibyaa cry AMA 2 rpynna Bokanbuas cryiMa «Belcantoy\n" +
                "13,25 «Belcanto» FAlapeinoga M.H.\n" +
                "\n" +
                "Hapetaosa MH.\n" +
                "\n" +
                "2 emena\n" +
                "\n" +
                "15.00- 3 rpynna BokalbHaa cryaHa 3 rpynna Bokanbuas ctyaita «Belcanto»\n" +
                "15.40 «Belcanto» JJaspiaoa M.H.\n" +
                "\n" +
                "Tlaptiazona MH.\n" +
                "15.45- 3 rpynna BokanbHaa cryaaa 3 rpynna Bokanbyaa ctyana «Belcanto»\n" +
                "16.25 «Belcanto» Aassinoza M.H.\n" +
                "\n" +
                "Zlastigopa M,H.\n" +
                "16.30- 4rpynna Boxanbnaa ctyoHa 4rpynna Bokajpnaa crygua «Belcanto»\n" +
                "17.10 «Belcanton Haseinosa M.U,\n" +
                "\n" +
                "Haseigona MH.\n" +
                "17.15- 4 rpynna Bowanbuaa cryaua 4 rpynna Bokanpyaa cryaua «Belcanto»\n" +
                "1755 «Belcanto» Aaseigosa MH.\n" +
                "\n" +
                "Haesiaoza M.H.\n" +
                "18.00- 5 rpynna BokanbHad cryaua 5 rpynna Boxanpnas ctyoHa «Belcanto»\n" +
                "18.40 «Belcanton Alapsiyjona MH,\n" +
                "\n" +
                "Jaspigora M.U.\n" +
                "18,45- 3 rpymina Bokanbyas cryana 5 rpynna Bokanbyaa cryaua «Belcanto»\n" +
                "19.25 «Belcanton Tapeijoaa MH.\n" +
                "\n" +
                "Haesiioka MM.\n" +
                "\n" +
                "SaBelyOWNi XyJOMeCTREHHO-3ICTETHYECKOrO HanpaBJeHHA ic Tycynos A.K. Kx\n" +
                "\n" +
                "Sau GBF\n" +
                "\n" +
                "\fY LBEPAK LALO\n" +
                "\n" +
                "Pacnucanne 3anaTuii\n" +
                "KpyaokK XopeorpaduseckHil ancaMOnp “Camra”\n" +
                "\n" +
                "TLLO: Kanesosa P.A\n" +
                "\n" +
                "xe | Bpema Tloneqenb uit Breprak Cpena\n" +
                "lemena\n" +
                "! | 9,00-9.40 2 rpynma XopeorpadbuseckHit 2 rpynma Xopeorpapayeckuit avicaM6JIb\n" +
                "ancamOmb “Cara” “Camra”’\n" +
                "Kane30za P.A Kane3zopa P.A\n" +
                "2 | 09,45- 2 rpymna XopeorpabuseckHii 2 rpynua Xopeorpadbyyeckit aHcam6.1b\n" +
                "10.25 ancam6ab “Camra” “Camera”\n" +
                "Kane3ona P.A Kane3ona PA\n" +
                "3 | 10.30-\n" +
                "11.10\n" +
                "4 | 11.15-\n" +
                "11.55\n" +
                "5 | 12.00-\n" +
                "12.40\n" +
                "6 | 12.45-\n" +
                "13,25\n" +
                "1 | 15,00- 1 rpynna Xopeorpadbwsecki | rpyana Xopeorpapuyeckuii\n" +
                "15.40 aucamOsab “Camera” ancamOnb “CamMra™\n" +
                "2 | 15.45- 1 rpynna Xopeorpadaseckuti 1 rpynna Xopeorpadayeckult\n" +
                "16.25 ancam6np “Camra” aucamM6nb “Camra”\n" +
                "Kanesora P.A Kane3opa P.A\n" +
                "3 | 16.30-\n" +
                "17.10\n" +
                "4 | 17,15-\n" +
                "17.55\n" +
                "| > | 18:00 3 rpynma Xopeorpauteckuit 4 rpynna Xopeorpaduyecknit 3 rpynna Xopeorpabuseckali 4 rpynna Xopeorpapuseckul\n" +
                "18:40 ancam6ub “Camra” ancaMOib “Camra” aucamM6ub “Camra” aucamOmb “Camra™\n" +
                "Kane30na P.A. Kane3ona P.A Kanesopa P.A\n" +
                "6 | 18:45 3 rpynna Xopeorpapuseck4h 4 rpynna Xopeorpapuseckuhi 3 rpyuna XopeorpabwieckHi 4 rpynma Xopeorpapayeckuit\n" +
                "19:25 ancam6an “Camra” ancamOnb “Camra” ancam6nb “Cara” aucam6an “Camra”\n" +
                "Kanesopa P.A Kane3opa P.A Kanezoza P.A\n" +
                "7 | 19:30\n" +
                "| 20:10\n" +
                "\n" +
                "3anenyrommii xyOKecTBeH\n" +
                "\n" +
                "HO-3CTETHUECKOTO HanpaBlhenun\n" +
                "\n" +
                "fe 7 Tycynog A.K.\n" +
                "$ase deat\n" +
                "\n" +
                "\fPacnucanue zanaATaii\n" +
                "Kaéuner CKypek xBLIyBD>\n" +
                "\n" +
                "THO: Maxutos H.C.,\n" +
                "Ne Bpems TlonenenpHak ] Bropaek Cpeaa Yersepr\n" +
                "| Diarunua\n" +
                "1 emena\n" +
                "1 9.00- |\n" +
                "9.40 |\n" +
                "2 09.45- 1 rpynna lrpynna «TorbiskyManaK»\n" +
                "10.25 «TorpiskyMamak) Maxutos H.C\n" +
                "Maxutos H.C\n" +
                "3 10.30- | rpynma lrpynna «TorpiskymanaK»\n" +
                "11.10 «TOFBIsKYMasiak»> Maxutos H.C\n" +
                "Maxuros H.C\n" +
                "4 11.15-\n" +
                "11.55\n" +
                "5 12.00-\n" +
                "12.40\n" +
                "6 12.45-\n" +
                "13,25\n" +
                "1 15.00-\n" +
                "15.40\n" +
                "2 15.45-\n" +
                "16,25\n" +
                "3 16.30- 2 rpynna«LlaxmaTsp» 1 rpynna «LlaxmMaTap» 2 rpynma«laxmMarTeL» 1 rpynma «[Daxmatoi»\n" +
                "17.10 Maoxutos H.C Maxnros [1.C., Maxutos H.C Maxuros H.C.,\n" +
                "4 17.15- 2 rpynna «IHaxmatep> 1 rpynua «LaxmMatbm 2 rpynna «laxmatEp 1 rpynna «laxmarep»\n" +
                "17.55 Maxxuros H.C Maxuros H.C., Maxutos H.C Maxutos H.C.,\n" +
                "3 18:00-\n" +
                "18:40\n" +
                "6 18:45-\n" +
                "19:25\n" +
                "SaBelyOouul xy j0KeCTBeEHHO-3CTeTHeCKOre HalpaB.ieHHa Tycynos A.K.\n" +
                "tf\n" +
                "\n" +
                "Sau Lip\n" +
                "\fPacnncanve 3ann THA\n" +
                "Kpyxox «[kon-crygHa Buxtropaa Taxonkosol»\n" +
                "TLQ0: Tuxonkosa B.B.,\n" +
                "\n" +
                "Ne] Bpenu. | Tlomeste smite Brapmne Chena | Yersepr l Usama I\n" +
                "1 emena\n" +
                "1 | 9.00-\n" +
                "9.40 7\n" +
                "2 | 09.45-\n" +
                "10.25 _— oe ee ee = _ ee _\n" +
                "3 | 10.30- } rpynna «[kona- cryaua\n" +
                "11.10 Bukropau THXOHKOBOi»\n" +
                "mall _ i = ee | ee Tuxoukosa B.B\n" +
                "4) 11,15- | 1 rpynna «Ll kona -ctyaHa\n" +
                "11.55 | Buxtopru THxonkosofi»\n" +
                ": _ _ 7 7 _ _ a = _ Tuxanxosa BB\n" +
                "5 | 12.00- 2 rpynna «kona -cTyaHa\n" +
                "12.40 Buxtoprn TaxouKosoiin\n" +
                "= —— os _ _ __ ‘Trxoukosa B.B\n" +
                "6 | 12.45- 2 rpynna «iLkona -cryansa\n" +
                "13.25 Buxtopun THxoHkosolt»\n" +
                "Tuxonkosa B.B\n" +
                "‘ — ve a — _ 2 cmena -\n" +
                "ty) 15.00- 3 rpynna «lkoma -cryaMa\n" +
                "15.40 Buxktopuy THxouKoBol»\n" +
                "_ Tuxankosa B.B\n" +
                "2 | 15.45- 3 rpynna «[[]Kona- cryaHa\n" +
                "16.25 Buxropun THxonKopoii»\n" +
                "Tuxoukosa B.B\n" +
                "3 | 16.30-\n" +
                "17.10\n" +
                "4) 17.15- 3 rpynna «ll kona- cryaua\n" +
                "17.55 Buxropnn THxcnkosor»\n" +
                "Tuxoukora B.B\n" +
                "5 | 18:00 3 rpyona «Lkona -cryaua\n" +
                "18:40 Buktopyn Taxonkosoli»\n" +
                "Tuxoukoga B.B\n" +
                "6) (8:45 2 rpynna «Liikona -cTyiHa 3 rpynma 1 rpynna «Lkona- cryana 2 rpynna «kona -cryqua | rpynna «LLIkoma -\n" +
                "19:25 | Buxtopun TaxonKxonoim «kona -cryaua | Buxtropyy Tuxouxosoli» Buktopuu THxoHkosoii» cTyaua BuKTopin\n" +
                "Tuxoukosa B.B BukropHy Tuxorkosa B.B TuxouKkosa B.B THXOHKOBOA»\n" +
                "TuxonKosoli» Trxonkora B.B\n" +
                "Tuxonkosa B.B\n" +
                "7) 19:30 2 rpynna «1 Lkona -ctyana 3 rpynna 1 rpynna «LL kona- eryaua 2 rpynna «llkona- cryuua T rpynna «Llosa -\n" +
                "20:10 | Baxroprn Tuxonxosofin «kona- ctyava | Buxropuu Tuxonkoroii» Bukropun Tuxonkosol» cTyaua Buktopun\n" +
                "Tuxonkosa B.B Buxtropuy Tuxonkosa B.B Tuxoukosa B.B THXxXOHKOBON»\n" +
                "TuxouKoaoli» Tuxorkona B.B\n" +
                "Tuxonkopa B.B\n" +
                "\n" +
                "3anenywomnli XY LOMKECTBEHHO-3CTeETHUeCKOroe HanpaBJIeHHa\n" +
                "\n" +
                "\fPacttucanne 3anATHii\n" +
                "Kpyxor «(paint acem\n" +
                "IV{O: Iengenko H.B..,\n" +
                "\n" +
                "Bpe Tloneqenbaux Bropuuk Cpeaa Yersepr Diatanua cy6éora\n" +
                "Mai\n" +
                "\n" +
                "1 cmena\n" +
                "\n" +
                "9.00-\n" +
                "9.40\n" +
                "\n" +
                "09.45-\n" +
                "10.25\n" +
                "\n" +
                "10.30-\n" +
                "11.10\n" +
                "\n" +
                "11.15-\n" +
                "11:55\n" +
                "\n" +
                "12.00-\n" +
                "12.40\n" +
                "\n" +
                "12.45-\n" +
                "13.25\n" +
                "\n" +
                "15.00-\n" +
                "15.40\n" +
                "\n" +
                "15.45-\n" +
                "16.25\n" +
                "\n" +
                "16.30- 1 rpynna «LI binaiinr ] rpynna «Memaiips\n" +
                "17.10 ammem» auiem>>\n" +
                "Ilesvenxo H.B. Iesyenxo H.B.\n" +
                "\n" +
                "17,15- 1 rpynna «LU binaiier | rpynna «Leimaiter\n" +
                "17-55 asleM)> aulem»\n" +
                "Illesuenxo H.B. ILlesyenxo H.B.\n" +
                "\n" +
                "Sapelylomni Xy10xKecTBeHHO-3CTeCTH4eCKOrO HanpaBlleHHa Gb a Tycynos ALK,\n" +
                "\n" +
                "Frau GBP ae\n" +
                "\n" +
                "\fPacnncanue 3anaTHii anrauiicioro s3bika\n" +
                "«Digital English»\n" +
                "\n" +
                "aN 7, o age\n" +
                "RQimrane. * LH\n" +
                "Dreux\n" +
                "\n" +
                "NO: Cmarynosa I.H.,\n" +
                "\n" +
                "Ne | Bpena | Tonteacman l Bropunk Cpeaa | Yersepr | Tiaranua\n" +
                "1 cmena\n" +
                "1 | 9,00- 1 rpynna 5 rpynna 1 rpynna S rpynna\n" +
                "9.40 AHTIMMCKHH a3BIK Aurmiicxuit 935K Anraniickuit 931k AHPIBACKHE a3bIK\n" +
                "«Digital English» «Digital English» «Digital English» «Digital English»\n" +
                "Cmarystopa T.H Cmarynopa TH Cmarystoaa TH Cmaryniospa TH\n" +
                "2 | 09.45- | I rpynna 5 rpynoa | rpynna 5 rpynna\n" +
                "10.25 AHPIKHCKHE a3bIk Anranfickuli a3bik AHraHMiickalt a3nrKk AHIHicKHii 93bik\n" +
                "«Digital English» «Digital English» «Digital English» «Digital English»\n" +
                "Cmarynosa IH Cmaryaopa I°.H Cmarynopa [.H Cmarynopa l’.H\n" +
                "3 | 10.30- | 2 rpynna 2 rpynna\n" +
                "11,10 AHPIMHCKMA ASbIK AHTiniickait a3B0K\n" +
                "«Digital English» «Digital English»\n" +
                "Cwarynoga I’.H Cmarynopa T.H\n" +
                "4 | 11.15- | 2 rpynna 2 rpyrna\n" +
                "11.55 AHriHlicKkHi a3bIk AXPAKHCKHH a3BuC\n" +
                "«Digital English» «Digital English»\n" +
                "Cmaryaoga TH Cwaryaoza lH\n" +
                "2 cmena\n" +
                "1 | 15.00- | 3 rpynna 3 rpymna\n" +
                "15.40 ABTIHMUCKHH A3BIK AXrAHiickHit 136K\n" +
                "«Digital English» «Digital English»\n" +
                "Cmarynopa TH Cmarvaosa TH\n" +
                "2 | 15.45- | 3 rpynna 3 rpynna\n" +
                "16.25 ANrniicKult a3pik Auramlickait a3prc\n" +
                "«Digital English» «Digital English»\n" +
                "Cmaryaopa [’.H Cmarynopa [.H\n" +
                "3 | 16.30- | 4 rpynna 6 rpynna 4rpynna 6 rpynna\n" +
                "17.10 AHTIBHcKHE A3bIK AHPIHHcKHit a3bIK AHTHiicKMi H3bIK AHTvIMicKHi a3bIK\n" +
                "«Digital English» «Digital English» «Digital English» «Digital English»\n" +
                "Cmarysosa [.H Cmarynoga I’.H Cmarysopa T.H Cmarystoza TH\n" +
                "4 | 17.15- | 4rpynna 6 rpynna 4 rpynna 6 rpynna\n" +
                "17,55 Anrniickuit a3b1K AHTIIMMCKHH ASbIK AHDAMiicKHit a3bIK AHIIHicKHi a3bIK\n" +
                "«Digital English» «Digital English» «Digital English «Digital English»\n" +
                "Cmarysosa T.H Cmarynona lH Cmarysoea T.H Cmaryiopa TH\n" +
                "5 | 18.00- 7 rpymna 7 rpynna\n" +
                "18.40 Auraniickait a3ik AHTIMMCKMI 33bIK\n" +
                "«Digital English» «Digital English»\n" +
                "Cmarynona I’.H Cmarynoga TH\n" +
                "\n" +
                "\f18.45- 7 rpynna 7 rpynna\n" +
                "19.25 ABIMHiicKHE ASbIK AHTIMMCKAa «SBI\n" +
                "«Digital English» «Digital English»\n" +
                "Cmarynona I.H Cmarynosal.H\n" +
                "Meroancr : Komeraanna H.b.\n" +
                "\n" +
                "SDMA\n" +
                "\n" +
                "\fPacnucanne 3anaTHit\n" +
                "TeaTp MOfbI H KpacoTsl «Kep6e3»\n" +
                "YOO —Konypamosa T.B.\n" +
                "\n" +
                "Ne | Bpema TloneaenbHnk Bropnnk Cpeaa Uersepr TintHnua Cy660Ta\n" +
                "| 09.00-\n" +
                "09.40\n" +
                "2 | 09.45-\n" +
                "10,25\n" +
                "3 10.30- 1 rpynma 2 rpynma\n" +
                "11.10 Tearp Mobi H Kpacotsl «Kep6e3» | Tearp moni uv kpacorsi «KepOe3»\n" +
                "Kongapauona T.B. Konapatiosa T.B.\n" +
                "4 1L.15- 1 rpyona 2 rpynna 1 rpynna\n" +
                "11.55 Tearp Mogsi K Kpacotsi «Kep6e3» | Tearp mogui u Kpacors! «Kep6e3» Teatp Moab H kpacoTst\n" +
                "Konapatosa T.B. Kongzpautoga T.B. «Kep6e3»\n" +
                "Kongpawiosa TB,\n" +
                "5 12.00- 1 epynna\n" +
                "12.40 Teatp MogbI H Kpacors!\n" +
                "«Kep6es»\n" +
                "Konapamosa T.B.\n" +
                "2 cmena\n" +
                "I 15.00-\n" +
                "15.40\n" +
                "2 | 15.45-\n" +
                "16.25\n" +
                "3 16.30- 3 rpynna 3 rpynna 2 rpynna\n" +
                "17.10 Tearp Mogbi m Kpacotsi «Kep6ea» | Tearp moabi v Kpacotsi «Kep6e3» Tearp MogbI 4 KpacoTsl\n" +
                "Kongpaurosa T.B. Konapauiosa T.B. «Kep6e3»\n" +
                "Kongpauiosa TB,\n" +
                "4 17.15- 3 rpynma 3 rpynna 2 rpynma\n" +
                "17,55 Teatp Mogbl H Kpacotsl «Kep6e3» | Tearp mogsi # Kpacotsi «Kep6es» Tearp MOouBI 4 Kpacorsi\n" +
                "Konapauiosa T.B. Konapauroza T.B. «Kepbe3»\n" +
                "Kouspamosa T.B.\n" +
                "Meroanct : Komerasanua H.b.\n" +
                "\n" +
                "Saux GSP\n" +
                "\n" +
                "\fPacnncanne 3anaTHii\n" +
                "\n" +
                "<Kac AKbrnaap»\n" +
                "HO- Amanxoa 3arpinap\n" +
                "No | Bpema | TloneqenbHnk | Bropnux Cpeza Uerpepr | Iaraana\n" +
                "1 cmena\n" +
                "\n" +
                "1 | 09.00-\n" +
                "\n" +
                "09.40\n" +
                "2 | 09.45- | rpynna\n" +
                "\n" +
                "10.25 «Kac AKbInsap»\n" +
                "\n" +
                "AMaHKOUL Sarbinap\n" +
                "\n" +
                "3 | 10.30- 1 rpynna\n" +
                "\n" +
                "11.10 «Kac AKbiHgap»\n" +
                "\n" +
                "Amamxon 3arpinap\n" +
                "\n" +
                "4 | 11.15-\n" +
                "\n" +
                "11.55\n" +
                "\n" +
                "2 cmena\n" +
                "\n" +
                "1 | 15.00- | rpynmna\n" +
                "\n" +
                "15.40 ; OKac AKBIHZap»\n" +
                "\n" +
                "Amano 3arpinap\n" +
                "\n" +
                "2 | 15.45- | rpynma\n" +
                "\n" +
                "16.25 OKac AKbinjiap»\n" +
                "\n" +
                "Amalpkon 3arpinap\n" +
                "\n" +
                "3 | 16.30-\n" +
                "\n" +
                "17.10\n" +
                "4 | 17.15-\n" +
                "\n" +
                "17,55\n" +
                "\n" +
                "Mertogucr Koureramuna Vb.\n" +
                "\n" +
                "Satu JOP Keay\n" +
                "\n" +
                "\fo\n" +
                "ew aC ble i\n" +
                "\n" +
                "Oo ET BEPIKILA TO\n" +
                "%\n" +
                "\n" +
                "oO ant tar\n" +
                "&\n" +
                "\n" +
                "Pacnncanne 3annTHii\n" +
                "Kpiseiktar ka3ak Tisi, lemenaix onep\n" +
                "TLO- Muip3abaesa IK.\n" +
                "\n" +
                "Ne | Bpemn [ TlonegebHnk BropHak | Cpena | Uerpepr | IsatHnna | Cy66orTa\n" +
                "1 cmena\n" +
                "1 | 09.00-\n" +
                "09.40\n" +
                "2 | 09.45-\n" +
                "10.25\n" +
                "3 | 10.30-\n" +
                "11.10\n" +
                "4 | 11.15-\n" +
                "11.55\n" +
                "2 cmena\n" +
                "1 | 14.00- | 1 rpynna 2rpynna | 3 rpynna\n" +
                "14.40 | «Keispikrei Ka3aK Tii» «KBISBIKTBI Ka3aK TUL» «eweHyik eHep»\n" +
                "2 | 14.45- | 1 rpynna 2 rpynna 3 rpynna\n" +
                "15.25 | «Kpi3bIKTBI Ka3ak TL» «KbISbIKTbI Ka3aKk TL» «lemengzix exep»\n" +
                "3 | 15.30- | 2 rpynna 3 rpynuna 1 rpynna\n" +
                "16.10 | «Kpi3bikTbr Kazak TL» «Weurengik exep» «KBIZbIKTBI Ka3ak THT»\n" +
                "4 | 16.15- | 2 rpynna 3 rpynna 1 rpyrma\n" +
                "16.55 | «KpispikTbr Ka3aKk Ti» «Wemengzix onep» «KBISBIKTEI Ka3ak Tii»\n" +
                "\n" +
                "Meroanet p if Komeraonua HB,\n" +
                "\n" +
                "f\n" +
                "\n" +
                "aun GBP Geer\n" +
                "\n" +
                "\fPacnucanue 3anaTuit\n" +
                "«Digital English»\n" +
                "ILO — Aury.aGnera T.A.\n" +
                "\n" +
                "tik\n" +
                "puacite Rog 2\n" +
                "we ww rHang, Mine\n" +
                "\n" +
                "=o Tee\n" +
                "2 potter\n" +
                "Po a fs\n" +
                "\n" +
                "eee\n" +
                "\n" +
                "HIKOJIBHHKOB»\n" +
                "\n" +
                "No | -Bpema Hlonegenbunk Bropunk Cpega Yersepr OsTuua CyiGora\n" +
                "1 09.00- 1 rpynna 1 rpyona.\n" +
                "\n" +
                "09.40 «Digital English» «Digital English»\n" +
                "2 09,.45- | rpynna 1 rpynna\n" +
                "\n" +
                "10,25 «Digital English» «Digital English»\n" +
                "3 10.30- 2 rpynona 2 rpynna\n" +
                "\n" +
                "11.10 «Digital English» «Digital English»\n" +
                "4 | 1115- 2 rpyona 2 rpynna\n" +
                "\n" +
                "11,55 «Digital English» «Digital English»\n" +
                "\n" +
                "2 cmena\n" +
                "\n" +
                "] 15.00-\n" +
                "\n" +
                "15.40\n" +
                "2 | 15.45-\n" +
                "\n" +
                "16.25\n" +
                "3 16.30-\n" +
                "\n" +
                "17.10\n" +
                "4 17.15-\n" +
                "\n" +
                "17.35\n" +
                "\n" +
                "Meroanct: Komeraanna HB.\n" +
                "\n" +
                "\fPacnncanne 3aHaTHi\n" +
                "\n" +
                "«OparopckKoe HckyccTBO»\n" +
                "IL1O- A6uapmMaxunosa AIK.\n" +
                "\n" +
                "z a opa JK.\n" +
                "\n" +
                "Ne | Bpema\n" +
                "\n" +
                "|\n" +
                "\n" +
                "TWonezenpnnnk\n" +
                "\n" +
                "Bropuuk\n" +
                "\n" +
                "| Cpena\n" +
                "\n" +
                "| Wersepr\n" +
                "\n" +
                "Cyi6oTa\n" +
                "\n" +
                "1 cmena\n" +
                "\n" +
                "1\n" +
                "\n" +
                "9.00-9.40\n" +
                "\n" +
                "9.45-10.25\n" +
                "\n" +
                "3\n" +
                "\n" +
                "10.30-11.10\n" +
                "\n" +
                "Opatopckoe\n" +
                "MCKYCCTBO\n" +
                "\n" +
                "11.15-11.55\n" +
                "\n" +
                "OparopcKkoe\n" +
                "HeKYCCTBO\n" +
                "\n" +
                "2 cmeHa\n" +
                "\n" +
                "15.00-15.40\n" +
                "\n" +
                "15.45-16.25\n" +
                "\n" +
                "Oparopekoe uckyccTBO\n" +
                "\n" +
                "16.30-17.10\n" +
                "\n" +
                "Opatopckoe uckyccTBo\n" +
                "\n" +
                "17.15-17,55\n" +
                "\n" +
                "18.00-18.40\n" +
                "\n" +
                "18.45-19.25\n" +
                "\n" +
                "~T) Os | On] Ga} boy]\n" +
                "\n" +
                "19.30-20.10\n" +
                "\n" +
                "Metoguct\n" +
                "\n" +
                "Komeraanna H.B.\n" +
                "\n" +
                "Sau YBP Heer\n" +
                "\n" +
                "\fPacnucanne 3aHATHH\n" +
                "«Turkish time», «English»\n" +
                "TIO —-Typesmrannepa M.2K,\n" +
                "\n" +
                "Ne | Bpesa NonerennHK Bropunk Cpena Yerpepr TsatTHnua Cy66oTa\n" +
                "1 09.00- | 1 rpynna . 1 rpynna 1 rpymma «English»\n" +
                "09.40 | «Turkish time» «Turkish time»\n" +
                "2 | 09.45- | | rpynna 1 rpynna | rpynma «English»\n" +
                "10.25 | «Turkish time»» «Turkish time»\n" +
                "3 10.30- | 1 rpynna «English»\n" +
                "11.10\n" +
                "4 | 11.15- | 1 rpynma «English»\n" +
                "11.55\n" +
                "2 cmena\n" +
                "1 15.00-\n" +
                "15.40\n" +
                "2 ee 2 rpynna «English» 2 rpynna «English»\n" +
                "16.25\n" +
                "3 Hrd 2 rpynua «English» 2 rpynna «English»\n" +
                "17.1\n" +
                "4 | 17.15\n" +
                "17.35\n" +
                "Mertoauct : Komerannna H.b.\n" +
                "\n" +
                "\f‘OJIbDHHKOB»\n" +
                "\n" +
                "2025 roy\n" +
                "Pacnucanne 2anATHii aBIAHiCKOrO A3bIKAa\n" +
                "«English club»\n" +
                "TLO: Crasimora C.H.\n" +
                "Ne | Bpema Tloneqenbunk Bropaak Cpeaa Werpepr Haruna Cy66ora\n" +
                "3 10.30- ] rpynna\n" +
                "11.10 Aunrnniicknit #361K «English club»\n" +
                "4 | 11.15- 1 rpynna | rpynna\n" +
                "11.55 Aurmmlickait a3pix «English Aunrimiickuii aspx «English club»\n" +
                "club»\n" +
                "5 | 12.00- 1 rpynna 2 rpynna\n" +
                "12.40 Auraniickuii #361 «English Anraulickuit a3bix «English club»\n" +
                "club»\n" +
                "6 | 12.45- 2 rpynna\n" +
                "13.25 Aurnnfickwit a3bix «English club»\n" +
                "] 15.00-\n" +
                "15.40\n" +
                "2 | 15.45-\n" +
                "16.25\n" +
                "3 16.30- 2 rpynna\n" +
                "17.10 Auraufickuit a3pik\n" +
                "«English club»\n" +
                "4 17,15- 2 rpymna\n" +
                "17.55 ANTIMiickHii A3bIK\n" +
                "«English club»\n" +
                "§ | 18.00- 3 rpynna 3 rpynma\n" +
                "18.40 Aurauiicnaii a3b1x «English Aurmuiickuit aspx «English\n" +
                "club» club»\n" +
                "6 | 18.45- 3 rpynna 3 rpymna\n" +
                "19,25 ea\n" +
                "\n" +
                "Auraniickaii a3pik «English\n" +
                "club»\n" +
                "\n" +
                "Anrmniicknit s3p1k «English\n" +
                "club»\n" +
                "\n" +
                "Merogner :\n" +
                "\n" +
                "Komeranuua HB.\n" +
                "\n" +
                "Sau JOP Ler\n" +
                "\n" +
                "KHHOBA JTK.\n" +
                "\n" +
                "\fPacnncanne 3anaTHH\n" +
                "«Ky pHadHCcTHKa H MeMaTeXHOJIOrHH)>\n" +
                "IO —Kenxebonatopa A.A.\n" +
                "\n" +
                "Ne | Bpema TlonegenbHnk Bropauk Cpeaa Yersepr Tarawa Cy6Gora\n" +
                "1 09.00- Irpynna\n" +
                "09.40 WK ypHamMcruka KSHE\n" +
                "MesHaTEXHOOTHA»\n" +
                "2 | 09.45- Irpynna\n" +
                "10.25 OKYpHasMcruKa #KoHE\n" +
                "MeaMaTeXHONOrTHA»\n" +
                "3 10.30- 2rpynna\n" +
                "11210 «Ky pHalMcTHKa KOH\n" +
                "MegHaTexHonornuy\n" +
                "4 11.15- 2rpynna\n" +
                "11.55 «WK ypHaliMcTHkKa %KaHeE\n" +
                "MeHaTeXHONOTHHy\n" +
                "2 cmena\n" +
                "1 15.00- lrpynna =\n" +
                "15.40 (OKypHanicTuka Kane MeAMaTeXHOAOTHA»\n" +
                "2 15.45- Irpynna\n" +
                "16.25 (OKy pHanverHka 2#9He MEAMATeX HOOP»\n" +
                "3 16,30- 2rpynna\n" +
                "17.10 «OK ypnanucTuka ane MegMarexHOMOrHu»\n" +
                "4 17,15- 2rpynna\n" +
                "17.55\n" +
                "\n" +
                "Kypyan HCTHKa 3KOHE MCN MaTexXHONOTHH»\n" +
                "\n" +
                "Merogner :‘\n" +
                "\n" +
                "Kowerasnua HB.\n" +
                "\n" +
                "Bou HBP faay\n" +
                "\n" +
                "\fPacnucanue 3ansTHit\n" +
                "«English»\n" +
                "TIO —Kyn6aepa AK,\n" +
                "\n" +
                "Ne: | Bpema Tlonegenbnnk Bropuuk Cpega YUersepr Naranua Bpema Cy66oTa\n" +
                "{| 9.00-9.40\n" +
                "2 | 09.45-10.25\n" +
                "3° | 10.30-11.10 | 1 rpynna ] rpynna\n" +
                "«English» «English»\n" +
                "4 | 14.15-11.55 | 1 rpynna | rpynna\n" +
                "«English» «English»\n" +
                "5 12.00-12.40\n" +
                "6 12.45-13.25\n" +
                "1 15.00-15.40\n" +
                "2 | 15.45-16.25\n" +
                "3 16.30-17.10\n" +
                "4 17,15-17,.55\n" +
                "5 18.00-18.40\n" +
                "6 18.45-19.25\n" +
                "j p]\n" +
                "Meroauct 1s y Koweranuua 4.5.\n" +
                "\n" +
                "= a\n" +
                "\n" +
                "Dove. Gfap eo 7\n" +
                "\n" +
                "\fPacttucanne 3anATHH\n" +
                "\n" +
                "«English Club»\n" +
                "IVO -Anrpmpunosa A.A.\n" +
                "\n" +
                "MN: | Bpema TloneqenbHuk Bropunk Cpena Uernepr Iisrunua Cy66oTa\n" +
                "1 | 09.00- 1 rpynna 1 rpynna\n" +
                "\n" +
                "09.40 «English Club» «English Club»\n" +
                "2 | 09.45- 1 rpynna 1 rpynna\n" +
                "\n" +
                "10.25 «English Club» «English Club»\n" +
                "3. | 10.30-\n" +
                "\n" +
                "11.10\n" +
                "4 | 11.15-\n" +
                "\n" +
                "11.55\n" +
                "\n" +
                "2 emeHa\n" +
                "\n" +
                "Il 15.00- 3 rpynna 3 rpynna\n" +
                "\n" +
                "15.40 «English Club» «English Club»\n" +
                "2 | 15.45- 3 rpynma 3 rpynna\n" +
                "\n" +
                "16.25 «English Club» «English Club»\n" +
                "3 | 16.30- 2 rpynna 2 rpynna\n" +
                "\n" +
                "17.10 «English Club» «English Club»\n" +
                "4 | 17.15- 2 rpynnma 2. rpynma\n" +
                "\n" +
                "17.55 «English Club» «English Club»\n" +
                "\n" +
                "Merognuct : Komerannua Hb.\n" +
                "\n" +
                "Shp kek]\n" +
                "\n" +
                "\fPacnncanne 3anATHH\n" +
                "«Oneparopckoe ACKyccTBO»\n" +
                "ILO —Eckengupos JI.b.\n" +
                "\n" +
                "No | Bpema Tloneaenbuuk Bropua Cpeaa Uersepr TistHnma Cy66oTa\n" +
                "1 09.00-\n" +
                "09.40\n" +
                "2 | 09.45-\n" +
                "10.25\n" +
                "2 emena\n" +
                "a 15.00-\n" +
                "15.40\n" +
                "4 | 15.45-\n" +
                "16.25\n" +
                "§ 16,30- 1 rpynna 1 rpynna\n" +
                "17.10 «OnmepaTopcKoe HCKyCCTBO» «OnepaTopckoe HCKyCCTBO»\n" +
                "Eckengupos JI.B, Eckengupor /I.B.\n" +
                "6 17.15- 1 rpynna 1 rpynna\n" +
                "17.55 «OneparopeKoe HcKyccTBO» «OnepatopcKoe HcKyccTBO»\n" +
                "Eckenzupos J].B. Ecxenmupos J.B.\n" +
                "\n" +
                "Merognuct :\n" +
                "\n" +
                "Komera.smua H.b.\n" +
                "\n" +
                "Sonu WP he\n" +
                "\n" +
                "\fCaG6ak KectTeci\n" +
                "\n" +
                "MexktTenke jeiinri JaHbIHAbIK CBIHBIOBI\n" +
                "KBBM: CazsBoxkacoga C.K.\n" +
                "\n" +
                "oS\n" +
                "\n" +
                "< ya*\n" +
                "\n" +
                "Ni | Bpemu | TloneqenpHuk | BropHuk | Cpeaa Uertsepr | Ilatunua\n" +
                "2 cmena\n" +
                "1 | 14.00-14.35 [- Ton I- Ton\n" +
                "2 | 14,.40-15.15 II - rom II- ton\n" +
                "3 | 15.20-15.55 II] - ton III - ton\n" +
                "Meroguct : Komeraanua K.B.\n" +
                "\n" +
                "\fPacnucanne 3anATHi\n" +
                "eGatuprit K.ry6 «Digital Urpaq»\n" +
                "\n" +
                "ILJ1O- Cyneimenos 2K.C.\n" +
                "\n" +
                "Ne | Bpema Tonesenbunk Bropunk Cpeaa Yerpepr Harnnua\n" +
                "1 cmeHa\n" +
                "\n" +
                "I 09.00-\n" +
                "09.40\n" +
                "\n" +
                "2 | 09.45-\n" +
                "10.25\n" +
                "\n" +
                "2 cmena\n" +
                "\n" +
                "3 | 17.15- 1 rp «Digital Urpaq» 1 rp «Digital Urpaq»\n" +
                "17.55 Cyneiimenos 3K.C. Cyneiimenor JK.C.\n" +
                "\n" +
                "4 | 18,00- 1 rp «Digital Urpaq» 1 rp «Digital Urpaq»\n" +
                "18.40 Cynetimenos 3K.C, Cyseiimenos K.C.\n" +
                "\n" +
                "5 | 18.45- 2rp «Digital Urpaq» 2 rp «Digital Urpaq»\n" +
                "19,25 Cynefimenos XK.C, Cynefimeros )K.C.\n" +
                "\n" +
                "6 | 19.30- 2 rp «Digital Urpaq» 2 rp «Digital Urpaq»\n" +
                "20.10 Cyneiimernos K.C. ss Cynelimenos XK.C.\n" +
                "\n" +
                "MetToanct : Komeraanua H.b.\n" +
                "\n" +
                "4au JOP\n" +
                "\n")
        );

        documents.add(new RetrievalDocument("учителя", "учителя", "\"ФИО и описание\":\n" +
                "\n" +
                "Заинутдинова Людмила\n" +
                "Заместитель директора по УВР художественной школы Дворца школьников.Стаж работы 30 лет. Образование Высшее. Петропавловский педагогический институт им. К.Д. Ушинского, Смольный институт Российской Академии образования г. Санкт-Петербурга по специальности искусствоведения (бакалавр искусства), педагог истории изобразительного искусства.\n" +
                "\n" +
                "Мустафин Дамир\n" +
                "Заместитель директора по IT направлению.Образование Высшее. Северо-Казахстанский государственный университет им. М. Козыбаева.\n" +
                "\n" +
                "Воробьева Лариса\n" +
                "Заведующая отделом научно-биологического направления.Стаж работы 32 года. Образование Высшее. Петропавловский педагогический институт им. К.Д. Ушинского Естественно-географического факультета по специальности учитель химии и биологии, квалификационная категория - высшая.\n" +
                "\n" +
                "Тусупов Акжол Каиржанович\n" +
                "Заведующий художественно-эстетического направления.Образование высшее Северо-Казахстанский государственный университет им. М. Козыбаева.\n" +
                "\n" +
                "Исмагулова Зарина Амангельдиевна\n" +
                "Заведующая методическим отделом.Образование высшее Северо-Казахстанский государственный университет им. М. Козыбаева.\n" +
                "\n" +
                "Мусамбекова Ляйлягуль Бақытжановна\n" +
                "Методист.Образование высшее. Омский государственный педагогический университет. Магистр социально-экономического образования. Профиль «Политология».\n" +
                "\n" +
                "Смагулова Эльнара Курмангазиевна\n" +
                "Методист.Образование высшее. Северо-Казахстанский государственный университет им. М. Козыбаева.\n" +
                "\n" +
                "Альназирова Куляш Мурзабековна\n" +
                "Методист.Образование высшее. Петропавловский педагогический институт им. К. Ушинского.\n" +
                "\n" +
                "Абраева Инеш Бахытжановна\n" +
                "Методист НБН направления.Образование высшее Северо-Казахстанский государственный университет им. М. Козыбаева.\n" +
                "\n" +
                "Жаксылык Адужан\n" +
                "IT– специалист. Образование высшее Костанайский инженерно-экономический университет им. М. Дулатова.\n" +
                "\n" +
                "Жиенбай Эсем Ердәулетқызы\n" +
                "IT– специалист. Образование высшее Кокшетауский университет имени Ш Уалиханова."));

        documents.add(new RetrievalDocument("хакатон", "хакатон 29", "ᅠ ᅠ ᅠ ᅠ ᅠ ᅠ ᅠ ᅠ ᅠ ᅠ ᅠ ᅠ ᅠ ᅠ ᅠ ᅠ ᅠ ᅠ ᅠ ᅠ ᅠ ᅠ ᅠ ᅠ ᅠ ᅠ ᅠ ᅠ ᅠ ᅠ ᅠ ᅠ ᅠ ᅠ ᅠ ᅠ, [29.10.25 13:30]\n" +
                "ПОЛОЖЕНИЕ о проведении хакатона\n" +
                "\n" +
                "Общие положения\n" +
                "\n" +
                "1. Настовцее положение определяет цель задачи, формат организации, сроки и порядок проведения, условия участия в хакатоне.\n" +
                "\n" +
                "2. Цель и задачи хакатона.\n" +
                "\n" +
                "развитие креативного мышления и опыта междисциплинарной командной работы\n" +
                "\n" +
                "по проектным решениям реальных бизнес-задач\n" +
                "\n" +
                "формирование банка данных проектных решений бизнес-вадлич обучение и развитие участников в области бизнеса;\n" +
                "\n" +
                "возможность живого общения молодежи с руководителями и экспертами ведущих компаний;\n" +
                "\n" +
                "отбор лучших проектов хакатона для дальнейшего рассмотрения и реализации. 3. Организатором хакатона является ГККП «Дворец шшкольникова.\n" +
                "\n" +
                "4. Организатор осуществляет подготовку и непосредственное проведение хакатона, формирует и утверждает состав жюри, ведет связь с участниками, анализирует и обобщает итоги хакатона и его проведение, а также информирует общественность о результатах хакатона.\n" +
                "\n" +
                "Термины и определения\n" +
                "\n" +
                "<\n" +
                "\n" +
                "5. Хахатном динамичное мероприятие, призванное стимулировать появление новых идей в выбранной предметной области и доведение их до реализации непосредственно на площадке хакатона. Формат хакатона позволяет объединить участников различных образовательных учреждений (школьники, студенты), с различными уровнями знаний и навыков, и дать им возможность познакомиться с новой предметной областью на практике. Творческая неформальная атмосфера хакатона способствует созданию новых идей и\n" +
                "\n" +
                "проектов, развитию ІТ-сообщества. Участник физическое лицо, действующее от своего имени и зарегистрировавшееся\n" +
                "\n" +
                "в соответствии с правилами Положения для участия в хакатоне. Команда групна участников и количестве двух человек, объединившихся для\n" +
                "\n" +
                "выполнения задания. Каждый участник может входить в состав только одной команды. Задание то, что назначено для выполнения командами за определенное время.\n" +
                "\n" +
                "Задание заключается в создании результата.\n" +
                "\n" +
                "Результат приложение/сервис или прототип приложения/сервиса, соответствующие критериям оценки результатов выполненного задания, включая описание функционала, дизайн, исходный код, созданный командой в результате выполнения задания и представленный к оценке жюри в указанный срок. Одна команда вправе представить только один результат.\n" +
                "\n" +
                "Победители команды, чьи результаты признанны лучшими в результате оценки жюри, на основании критериев, установленных настоящим Положением.\n" +
                "\n" +
                "Жори группа лиц, осуществляющих оценку выполненных заданий и определяющая победителей хакатона. В состав жюри входят независимые эксперты, компетентные в IT сфере\n" +
                "\n" +
                "Участники хакатона.\n" +
                "\n" +
                "6. Участие в хакатоне могут принять учащиеся школ города Петропавловска и возрасте от 14 до 18 лет, а также студенты 1-2 курсов колледжей и университета до 18 лет.\n" +
                "\n" +
                "ᅠ ᅠ ᅠ ᅠ ᅠ ᅠ ᅠ ᅠ ᅠ ᅠ ᅠ ᅠ ᅠ ᅠ ᅠ ᅠ ᅠ ᅠ ᅠ ᅠ ᅠ ᅠ ᅠ ᅠ ᅠ ᅠ ᅠ ᅠ ᅠ ᅠ ᅠ ᅠ ᅠ ᅠ ᅠ ᅠ, [29.10.25 13:30]\n" +
                "Программа проведення закатона\n" +
                "\n" +
                "28 октября 2025 года\n" +
                "\n" +
                "09:30-09:50\n" +
                "\n" +
                "Регистрация команд\n" +
                "\n" +
                "10:00-10:30\n" +
                "\n" +
                "Открытие закатона, оглашение задания\n" +
                "\n" +
                "10:30-13:00\n" +
                "\n" +
                "Выполнение задания хакатона\n" +
                "\n" +
                "13:00-13:30\n" +
                "\n" +
                "Обеденный перерыв\n" +
                "\n" +
                "13:30-16:30\n" +
                "\n" +
                "Продолжение выполнения задания хакатона\n" +
                "\n" +
                "29 октября 2025 года\n" +
                "\n" +
                "16:30-17:00\n" +
                "\n" +
                "Подведение итогов первого дня хакатона\n" +
                "\n" +
                "10:00-12:30\n" +
                "\n" +
                "12:30-13:00\n" +
                "\n" +
                "Продолжение выполнения задания хакатона. Завершение\n" +
                "\n" +
                "13:00-16:30\n" +
                "\n" +
                "Обеденный перерыв\n" +
                "\n" +
                "16:30-17:00\n" +
                "\n" +
                "Презентация выполненных заданий командами участниками\n" +
                "\n" +
                "Подведение итогов хакатона. Определение команд победителей.\n" +
                "\n" +
                "Закрытие хакатона. Награждение\n" +
                "\n" +
                "ᅠ ᅠ ᅠ ᅠ ᅠ ᅠ ᅠ ᅠ ᅠ ᅠ ᅠ ᅠ ᅠ ᅠ ᅠ ᅠ ᅠ ᅠ ᅠ ᅠ ᅠ ᅠ ᅠ ᅠ ᅠ ᅠ ᅠ ᅠ ᅠ ᅠ ᅠ ᅠ ᅠ ᅠ ᅠ ᅠ, [29.10.25 13:30]\n" +
                "В составе одной команды 3 участника.\n" +
                "\n" +
                "9. От одного образовательного учреждения разрешается подать не более 3-х команд. От одного руководителя не более 2-х команд.\n" +
                "\n" +
                "10. Количество команд ограниченное. Организатор вправе остановить прием заявок на участие в закатоне раньше установленного срока, если количество заявок превысило технические возможности хакатона.\n" +
                "\n" +
                "11. Организатор вправе перенести дату проведения хаютона в случае форс-мажорных обстоятельств\n" +
                "\n" +
                "Сроки и порядок проведения хакатона\n" +
                "\n" +
                "12. Хакатон проводится 28-29 октября 2025 года по Дворце школьников.\n" +
                "\n" +
                "Начало в 10:00 часов. Регистрация команд с 09.30 на 1 этаже.\n" +
                "\n" +
                "13. Подробная программа хакатона представлена в приложении (Таблица 1) 14. Регистрация команд производится посредством Google Формы по ссылке\n" +
                "\n" +
                "https://forms.gle/96e9CPrysłzipp2yD8 до 20:00 часов 23 октября 2025 года.\n" +
                "\n" +
                "15. Заявки, поступившие позднее указанного срока, обозначенного в п.14 настояiero Положения, будут отклонены.\n" +
                "\n" +
                "16. За достоверность данных, указанных в заявке, несут ответственность руководители участников. Дипломы победителей и сертификаты за участие будут\n" +
                "\n" +
                "заподняться на основании данных, указанных в заявках и исправлению не подлежат. 17. Организатор хакатона свяжется по указанному в заявке телефону с\n" +
                "\n" +
                "руководителями всех зарегистрировавшихся участников для подтверждения участия в хакатоне.\n" +
                "\n" +
                "Условия и порядок проведения закатона\n" +
                "\n" +
                "18. Участие в хакатоне бесплатное.\n" +
                "\n" +
                "19. За отведенное время участники доложны выполнить основное задание хакатона подготовить презентацию выполненного задания и презентовать пори. Задание хакатона будет оглашено в день проведения.\n" +
                "\n" +
                "20. Участники должны обеспечить свою команду ноутбуком, зарядными. устройствами.\n" +
                "\n" +
                "Порядок и критерии оценки результатов\n" +
                "\n" +
                "21. Жюри оценивает результат команды. Итоги хакатона подводятся на основания оценки результатов команд.\n" +
                "\n" +
                "22. Оценка результатов выполненного задания осуществляется жюри по 5-балльной шкале по совокупности следующих критериев:\n" +
                "\n" +
                "соответствие выполненного задания заявленной тематике хакатона\n" +
                "\n" +
                "оригинальность и инновационность проектного решения\n" +
                "\n" +
                "- проработанность технического решения\n" +
                "\n" +
                "экономическая эффективность предложенного проектного решения - качество подготовки презентационных материалов и выступления\n" +
                "\n" +
                "23. По итогам результатов оценки команд, жюри определяет победителей 1, 2 и 3 степени. Победители будут награждены соответствующими дипломами и ценными подарками.\n" +
                "\n" +
                "Телефон для справок: 8(7152) 34-02-44\n" +
                "\n" +
                "Проезд участников до места проведенка хакатона и обратно, питание и любые другие расходы, связанные с участием, оплачиваются")
        );

        log.info("Created {} base documents", documents.size());
        return documents;
    }
}
