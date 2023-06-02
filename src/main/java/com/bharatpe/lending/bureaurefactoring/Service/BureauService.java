package com.bharatpe.lending.bureaurefactoring.Service;

import com.bharatpe.lending.bureaurefactoring.dto.*;
import com.bharatpe.lending.bureaurefactoring.enums.Bureau;
import com.bharatpe.lending.bureaurefactoring.enums.BureauLoanType;
import com.bharatpe.lending.bureaurefactoring.enums.BureauTradeType;
import com.bharatpe.lending.bureaurefactoring.exception.BureauCallMaskedApiException;
import com.bharatpe.lending.bureaurefactoring.nfi.ExperianNfiCalculator;
import com.bharatpe.lending.bureaurefactoring.utils.BureauResponseUtil;
import com.bharatpe.lending.bureaurefactoring.utils.CrifResponseUtil;
import com.bharatpe.lending.bureaurefactoring.utils.ExperianResponseUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
public class BureauService {

    private static final double LOAN_ENQUIRY_WEIGHT = 0.2;
    private static final double DELINQUENCY_WEIGHT = 0.2;
    private static final double LOAN_COUNT_WEIGHT = 0.2;
    private static final double LOAN_TYPE_WEIGHT = 0.2;
    private static final double UNSECURED_LOAN_WEIGHT = 0.1;
    private static final double CREDIT_HISTORY_WEIGHT = 0.1;
    private static final double BBS_MULTIPLIER = 300;
    @Autowired
    MongetService mongetService;
    @Autowired
    ExperianService experianService;
    @Autowired
    CrifService crifService;
    @Autowired
    ExperianNfiCalculator experianNfiCalculator;
    @Autowired
    AuditService auditService;

    Logger logger = LoggerFactory.getLogger(BureauService.class);

    @Value("${experian.vs.crif.load:1}")
    Double loadBalancerValue;

    public static Calendar getCalenderInstance() {
        return Calendar.getInstance(TimeZone.getDefault());
    }
    public static Date getDatePlusDays(Date date, int days) {
        Calendar calendar = getCalenderInstance();
        calendar.setTime(date);
        calendar.add(Calendar.DAY_OF_WEEK, days);
        return calendar.getTime();
    }
    private BureauResponseDTO fetchBureauResponse(BureauDataDTO bureauResponse, BureauResponseDTO bureauResponseDTO) {
        if (bureauResponse != null && bureauResponse.getBureau_response() != null) {
            bureauResponseDTO = fetchBureauData(bureauResponse, bureauResponseDTO);
        } else {
            bureauResponseDTO.setIsNTC(Boolean.TRUE);
        }
        return bureauResponseDTO;
    }

    private BureauResponseDTO fetchBureauData(BureauDataDTO bureauData, BureauResponseDTO bureauResponseDTO) {
        try {
            BureauResponseUtil bureauResponseUtil;
            if (Bureau.EXPERIAN.toString().equalsIgnoreCase(bureauData.getBureau_type())) {
                bureauResponseUtil = new ExperianResponseUtil(bureauData.getBureau_response());
            } else if (Bureau.CRIF.toString().equalsIgnoreCase(bureauData.getBureau_type())) {
                bureauResponseUtil = new CrifResponseUtil(bureauData.getBureau_response());
            } else {
                bureauResponseUtil = new ExperianResponseUtil(bureauData.getBureau_response());
            }

            if (bureauResponseUtil.isValid(bureauData.getPancard(), bureauData.getMobile().toString())) {
                bureauResponseDTO = fetchBureauVariable(bureauResponseUtil, bureauData,bureauResponseDTO);
                if (Objects.nonNull(bureauResponseDTO)) {
                    bureauResponseDTO.setBureauData(bureauResponseUtil != null && bureauResponseUtil.getResponse() != null ? bureauResponseUtil.getResponse() : null);
                }
//                BureauDataDTO bureauDataDTO = BureauDataDTO.builder()
//                        .mobile(bureauData.getMobile())
//                        .bureauVariables(bureauResponseDTO.getVariables())
//                        .build();
//                bureauResponseService.updateBureauData(bureauDataDTO);
                bureauResponseDTO.setBureauType(bureauData.getBureau_type());
                return bureauResponseDTO;
            }
        } catch (Exception e) {
            logger.error("Exception While Calculated BureauData Variable:{}", e);
        }
        return bureauResponseDTO;
    }

    private void calculateBBS(BureauResponseUtil bureauResponseUtil, BureauResponseDTO.BureauVariables variables, BureauDataDTO bureauData) {
        try {
            Map<String, Object> bbs = bureauResponseUtil.getBBSCalculationDetails();
            NfiCalculationDetailsDto nfiCalculationDetailsDto = (NfiCalculationDetailsDto) bbs.get("debtAndIncome");
            Double debt = nfiCalculationDetailsDto.getDebt();
            Double income = nfiCalculationDetailsDto.getIncome();
            Double incomePostpe = nfiCalculationDetailsDto.getIncomePostPe();

            int loanEnquires3mon = bureauResponseUtil.countLoanEnquiriesInLast3Months();
            int delinquencyCount6mon = (Integer) bbs.get("delinquencyCount6mon");
            int loanSanctioned3mon = (Integer) bbs.get("loanSanctioned3mon");
            int unsecuredLoanCount6mon = (Integer) bbs.get("unsecuredLoanCount6mon");
            double unsecuredLoanRatio6mon = unsecuredLoanCount6mon * 1.0 / 6;
            Set<Integer> loanTypes = (Set<Integer>) bbs.get("loanTypes");
            Date reportDate = bureauResponseUtil.getReportDate();
            Date minOpenDate = (Date) bbs.get("minOpenDate");
            int creditHistory = (int) ChronoUnit.MONTHS.between(
                    minOpenDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate(),
                    reportDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
            );
            double bbsScore = (
                    (LOAN_ENQUIRY_WEIGHT * ExperianResponseUtil.loanEnquiresScore(loanEnquires3mon))
                            + (DELINQUENCY_WEIGHT * ExperianResponseUtil.delinquencyScore(delinquencyCount6mon))
                            + (LOAN_COUNT_WEIGHT * ExperianResponseUtil.loanSanctionedScore(loanSanctioned3mon))
                            + (LOAN_TYPE_WEIGHT * ExperianResponseUtil.loanTypesScore(loanTypes.size()))
                            + (UNSECURED_LOAN_WEIGHT * ExperianResponseUtil.unsecuredLoanScore(unsecuredLoanRatio6mon))
                            + (CREDIT_HISTORY_WEIGHT * ExperianResponseUtil.historyScore(creditHistory))
            ) * BBS_MULTIPLIER;
            variables.setBbs(
                    Math.max(bbsScore, 0d)
            );
            logger.info("income: {}, debt: {} for mobile: {}",income,debt,bureauData.getMobile());
            Double nfi = experianNfiCalculator.getNetFreeIncome(income,debt);
            Double nfiPostpe = experianNfiCalculator.getNetFreeIncome(incomePostpe,debt);
            nfiCalculationDetailsDto.setNfi(nfi);
            nfiCalculationDetailsDto.setNfiPostpe(nfiPostpe);
            nfiCalculationDetailsDto.setMobile(bureauData.getMobile());
            nfiCalculationDetailsDto.setHit_id(bureauData.getHit_id());
            nfiCalculationDetailsDto.setFirstName(bureauData.getFirstName());
            nfiCalculationDetailsDto.setLastName(bureauData.getLastName());
            nfiCalculationDetailsDto.setPancard(bureauData.getPancard());
            logger.info("nfi: {} for mobile: {}",nfiCalculationDetailsDto,bureauData.getMobile());
            auditService.auditNfiCalculationDetails(nfiCalculationDetailsDto, "nfi_calculation_details_audit");
            Double activeUslPos = (Double) bbs.get("activeUnsecuredLoanAmount");
            variables.setNfi(nfi);
            variables.setNfiPostpe(nfiPostpe);
            variables.setLoanEnquires3mon(loanEnquires3mon);
            variables.setLoanEnquires3monScore(ExperianResponseUtil.loanEnquiresScore(loanEnquires3mon));
            variables.setDelinquencyCount6mon(delinquencyCount6mon);
            variables.setDelinquencyCount6monScore(ExperianResponseUtil.delinquencyScore(delinquencyCount6mon));
            variables.setLoanSanctioned3mon(loanSanctioned3mon);
            variables.setLoanSanctioned3monScore(ExperianResponseUtil.loanSanctionedScore(loanSanctioned3mon));
            variables.setLoanTypeSize(loanTypes.size());
            variables.setLoanTypeSizeScore(ExperianResponseUtil.loanTypesScore(loanTypes.size()));
            variables.setUnsecuredLoanRatio6mon(unsecuredLoanRatio6mon);
            variables.setUnsecuredLoanRatio6monScore(ExperianResponseUtil.unsecuredLoanScore(unsecuredLoanRatio6mon));
            variables.setCreditHistory(creditHistory);
            variables.setCreditHistoryScore(ExperianResponseUtil.historyScore(creditHistory));
            variables.setDebt(debt);
            variables.setIncome(income);
            variables.setActiveUslPos(activeUslPos);

        } catch (Exception e) {
            logger.error("Exception while calculating BBS", e);
        }
    }

    private Boolean checkThinLine(BureauResponseDTO.BureauVariables variables) {
        return variables.getHlCount() == 0 && variables.getAlCount() == 0 &&
                (variables.getPlCount() == 0 || variables.getMaxPLAmount() < 50000d) &&
                (variables.getBlCount() == 0 || variables.getMaxBLAmount() < 50000d);
    }

    private BureauTradeType getTradeType(BureauResponseDTO.BureauVariables variables) {
        if (variables.getCreditCardCount().equals(0)) {
            return BureauTradeType.NON_CARDED;
        } else if (variables.getCreditCardCount().equals(1)) {
            return BureauTradeType.SINGLE;
        } else {
            return BureauTradeType.MULTIPLE;
        }
    }
    private BureauResponseDTO fetchBureauVariable(BureauResponseUtil bureauResponseUtil, BureauDataDTO bureauData, BureauResponseDTO bureauResponseDTO) {
        try {
            if (Objects.isNull(bureauResponseUtil) || Objects.isNull(bureauResponseUtil.getResponse())) {
                bureauResponseDTO.setIsNTC(Boolean.TRUE);
                return bureauResponseDTO;
            }
            bureauResponseDTO.setIsNTC(!bureauResponseUtil.getETC());
            BureauResponseDTO.BureauVariables variables = BureauResponseDTO.BureauVariables.builder()
                    .bureauScore(bureauResponseUtil.getBureauScore())
                    .email(bureauResponseUtil.getEmail())
                    .address(bureauResponseUtil.getAddress())
                    .gender(bureauResponseUtil.getGender())
                    .bureauVintage(bureauResponseUtil.fetchBureauVintage())
                    .creditCardCount(bureauResponseUtil.getLoanCount(BureauLoanType.CREDIT_CARD))
                    .ccVintage(bureauResponseUtil.getVintage(BureauLoanType.CREDIT_CARD))
                    .twlCount(bureauResponseUtil.getLoanCount(BureauLoanType.TWO_WHEELER))
                    .glCount(bureauResponseUtil.getLoanCount(BureauLoanType.GOLD_LOAN))
                    .cdlCount(bureauResponseUtil.getLoanCount(BureauLoanType.CONSUMER_DURABLE))
                    .plCount(bureauResponseUtil.getLoanCount(BureauLoanType.PERSONAL_LOAN))
                    .hlCount(bureauResponseUtil.getLoanCount(BureauLoanType.HOME_LOAN))
                    .alCount(bureauResponseUtil.getLoanCount(BureauLoanType.AUTO_LOAN))
                    .blCount(bureauResponseUtil.getLoanCount(BureauLoanType.BUSINESS_LOAN))
                    .olCount(bureauResponseUtil.getLoanCount(BureauLoanType.OTHER_LOAN))
                    .excludedLoanCount(bureauResponseUtil.getLoanCount(BureauLoanType.EXCLUDED_LOAN))
                    .totalLoanCount(bureauResponseUtil.getTotalLoanCount())
                    .age(bureauResponseUtil.getAge())
                    .dob(bureauResponseUtil.getDOB())
                    .maxDpd3Months(bureauResponseUtil.getMaxDPD(3))
                    .maxDpd6Months(bureauResponseUtil.getMaxDPD(6))
                    .maxDpd12Months(bureauResponseUtil.getMaxDPD(12))
                    .maxDpd18Months(bureauResponseUtil.getMaxDPD(18))
                    .maxDpd24Months(bureauResponseUtil.getMaxDPD(24))
                    .writtenOffLast12Months(bureauResponseUtil.writtenOffLast12Months())
                    .maxOverdueAmount6Months(bureauResponseUtil.getMaxOverdueAmount())
                    .unsecuredEnquiries3Months(bureauResponseUtil.countUnsecuredLoanEnquiries(3))
                    .securedEnquiries3Months(bureauResponseUtil.countSecuredLoanEnquiries(3))
                    .unsecuredEnquiries6Months(bureauResponseUtil.countUnsecuredLoanEnquiries(6))
                    .maxHLAmount(bureauResponseUtil.maxLoanAmount(BureauLoanType.HOME_LOAN))
                    .maxALAmount(bureauResponseUtil.maxLoanAmount(BureauLoanType.AUTO_LOAN))
                    .maxPLAmount(bureauResponseUtil.maxLoanAmount(BureauLoanType.PERSONAL_LOAN))
                    .maxBLAmount(bureauResponseUtil.maxLoanAmount(BureauLoanType.BUSINESS_LOAN))
                    .maxOLAmount(bureauResponseUtil.maxLoanAmount(BureauLoanType.OTHER_LOAN))
                    .maxCCLimit(bureauResponseUtil.maxLoanAmount(BureauLoanType.CREDIT_CARD))
                    .minCCLimit(bureauResponseUtil.minLoanAmount(BureauLoanType.CREDIT_CARD))
                    .totalHLAmount(bureauResponseUtil.totalLoanAmount(BureauLoanType.HOME_LOAN))
                    .totalALAmount(bureauResponseUtil.totalLoanAmount(BureauLoanType.AUTO_LOAN))
                    .totalPLAmount(bureauResponseUtil.totalLoanAmount(BureauLoanType.PERSONAL_LOAN))
                    .totalBLAmount(bureauResponseUtil.totalLoanAmount(BureauLoanType.BUSINESS_LOAN))
                    .totalOLAmount(bureauResponseUtil.totalLoanAmount(BureauLoanType.OTHER_LOAN))
                    .totalCCLimit(bureauResponseUtil.totalLoanAmount(BureauLoanType.CREDIT_CARD))
                    .reportDate(bureauResponseUtil.getReportDate().getTime())
                    .unsecuredLoanUtilization(bureauResponseUtil.unsecuredLoanUtilization())
                    .maxCCBalance(bureauResponseUtil.maxCurrentBalance(BureauLoanType.CREDIT_CARD))
                    .totalEnquiryLast12Month(bureauResponseUtil.totalEnquiryLastXMonth(12))
                    .creditCardOverDuePast3Month(bureauResponseUtil.creditCardOverDuePastXMonth(3))
                    .creditCardOverDuePast6Month(bureauResponseUtil.creditCardOverDuePastXMonth(6))
                    .creditCardOverDuePast9Month(bureauResponseUtil.creditCardOverDuePastXMonth(9))
                    .creditCardOverDuePast12Month(bureauResponseUtil.creditCardOverDuePastXMonth(12))
                    .nonCreditCardOverduePast3Month(bureauResponseUtil.nonCreditOverDuePastXMonthAndActive(3))
                    .nonCreditCardOverduePast6Month(bureauResponseUtil.nonCreditOverDuePastXMonthAndActive(6))
                    .nonCreditCardOverduePast9Month(bureauResponseUtil.nonCreditOverDuePastXMonthAndActive(9))
                    .nonCreditCardOverduePast12Month(bureauResponseUtil.nonCreditOverDuePastXMonthAndActive(12))
                    .settleLoanPast12Month(bureauResponseUtil.settleLoanPastXMonth(12))
                    .settleLoanPast24Month(bureauResponseUtil.settleLoanPastXMonth(24))
                    .maxCreditCardTradeMoreThan60(bureauResponseUtil.maxCreditCardTradeMoreThan60(60))
                    .maxNonCreditCardTradeMoreThan60(bureauResponseUtil.maxNonCreditCardTradeMoreThan60(60))
                    .closedLoanWithOverDueLast12Month(bureauResponseUtil.closedLoanWithOverDueLastXMonth(12))
                    .activeGoldLoanGreaterThan20K(bureauResponseUtil.activeGoldLoan(20000d))
                    .activePersonalLoanGreaterThan10k(bureauResponseUtil.activePersonalLoan(10000d))
                    .tradeDpd15to29Last3month(bureauResponseUtil.tradeDpdLessThan(15, 29, 3))
                    .tradeDpd30to59Last6month(bureauResponseUtil.tradeDpdLessThan(30, 59, 6))
                    .tradeDpd60to89Last12month(bureauResponseUtil.tradeDpdLessThan(60, 89, 12))
                    .tradeDpd30to59MoreThan6Month(bureauResponseUtil.tradeDpdMoreThan(30, 59, 6))
                    .tradeDpd60to89MoreThan12Month(bureauResponseUtil.tradeDpdMoreThan(60, 89, 12))
                    .subDpdGreaterThan90Last18Month(bureauResponseUtil.subDPDLessThan(90, 18))
                    .subDpdGreaterThan90MoreThan18Month(bureauResponseUtil.subDPDMoreThan(90, 18))
                    .unsecuredEnquiries6Months(bureauResponseUtil.countUnsecuredLoanEnquiriesInLast6Months())
                    .tradeDpdGreaterThan90Last24Month(bureauResponseUtil.tradeDpdLessThan(90, 180, 24))
                    .loanSettlement(bureauResponseUtil.getLoanSettlement())
                    .creditScoreReportDetailDTO(bureauResponseUtil.getCreditDetailReport(bureauResponseUtil.getResponse()))
                    .loanAndCreditCardDetailDTO(bureauResponseUtil.getLoanAndCreditDetail(bureauResponseUtil.getResponse()))
                    .build();
            variables.setThinLine(checkThinLine(variables));
            variables.setTradeType(getTradeType(variables));
            calculateBBS(bureauResponseUtil, variables, bureauData);
            bureauResponseDTO.setVariables(variables);
            return bureauResponseDTO;
        } catch (Exception e) {
            logger.error("Exception while fetching Bureau Variable for merchant:{} {} {} {}", bureauData.getMobile(), e.getMessage(), Arrays.asList(e.getStackTrace()), e);
        }
        return bureauResponseDTO;
    }
    public ResponseDTO<BureauResponseDTO> BureauDetails(ExperianDetailsDTO requestDto, Long days, boolean requireBureauResponse) throws Exception {
        try {
            if (requestDto.getMobile().toString().length() == 12) {
                String mob = requestDto.getMobile().toString().substring(2);
                requestDto.setMobile(Long.valueOf(mob));
            }
            //Boolean onePercentUser = (requestDto.getMobile() % 10 == 9) ? Boolean.TRUE : Boolean.FALSE;
            Boolean onePercentUser = Boolean.FALSE;
            BureauDataDTO merchantConsent = mongetService.getConsentData(requestDto.getMobile().toString());
            if (onePercentUser && Objects.nonNull(merchantConsent) && Objects.nonNull(merchantConsent.getSource()) &&
                    merchantConsent.getSource().equalsIgnoreCase("EASY_LOAN")
                    && Objects.isNull(merchantConsent.getConsentDetails())) {
                return new ResponseDTO<>(false, "Merchant consent not available for merchant");
            }
            BureauDataDTO bureauResponse;
            BureauResponseDTO bureauResponseDto = null;
            if (onePercentUser) {
                bureauResponseDto = BureauResponseDTO.builder()
                        .pancard(requestDto.getPanCard())
                        .mobile(requestDto.getMobile().toString())
                        .consentDate(Objects.nonNull(merchantConsent) && Objects.nonNull(merchantConsent.getConsentDetails())
                                && Objects.nonNull(merchantConsent.getConsentDetails().getConsentDate())
                                ? merchantConsent.getConsentDetails().getConsentDate() : null)
                        .build();
            } else {
                bureauResponseDto = BureauResponseDTO.builder()
                        .pancard(requestDto.getPanCard())
                        .mobile(requestDto.getMobile().toString())
                        .build();
            }

            double loadBalancer = Math.random();
            if (loadBalancer < loadBalancerValue) {
                bureauResponse = experianService.getExperian(requestDto.getFirstName(), requestDto.getLastName(),
                        requestDto.getMobile(), requestDto.getPanCard(), days, requestDto.getSource());
                if (bureauResponse == null || bureauResponse.getBureau_response() == null) {
                    logger.info("Primary Experian Fail go to CRIF for mobile:{}", requestDto.getMobile());
                    bureauResponse = crifService.getCrif(requestDto.getFirstName(), requestDto.getLastName(),
                            requestDto.getMobile(), requestDto.getPanCard(), days, requestDto.getSource());
                }
            } else {
                bureauResponse = crifService.getCrif(requestDto.getFirstName(), requestDto.getLastName(),
                        requestDto.getMobile(), requestDto.getPanCard(), days, requestDto.getSource());
                if (bureauResponse == null || bureauResponse.getBureau_response() == null) {
                    logger.info("Primary Crif Fail go to Experian for mobile:{}", requestDto.getMobile());
                    bureauResponse = experianService.getExperian(requestDto.getFirstName(), requestDto.getLastName(),
                            requestDto.getMobile(), requestDto.getPanCard(), days, requestDto.getSource());
                }
            }
            bureauResponseDto = fetchBureauResponse(bureauResponse, bureauResponseDto);
            logger.info("bureau response dto: {}",bureauResponseDto);
            if (Objects.nonNull(bureauResponseDto)) {
                if(Objects.isNull(bureauResponseDto.getBureauData())) {
                    bureauResponseDto.setIsNTC(Boolean.TRUE);
                }
                if(!requireBureauResponse) {
                    bureauResponseDto.setBureauData(null);
                }
            }

            return new ResponseDTO<>(bureauResponseDto);
        } catch(BureauCallMaskedApiException e){
            throw (e);
        } catch (Exception e) {
            logger.error("Exception while requesting bureau response for mobile : {}", requestDto.getMobile(), e);
            return new ResponseDTO<>(false, "Something went wrong");
        }
    }

}
