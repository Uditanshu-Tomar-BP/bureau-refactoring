package com.bharatpe.lending.bureaurefactoring.Controller;

import com.bharatpe.lending.bureaurefactoring.Service.BureauService;
import com.bharatpe.lending.bureaurefactoring.constants.BureauConstants;
import com.bharatpe.lending.bureaurefactoring.dto.BureauResponseDTO;
import com.bharatpe.lending.bureaurefactoring.dto.ExperianDetailsDTO;
import com.bharatpe.lending.bureaurefactoring.dto.ResponseDTO;
import com.bharatpe.lending.bureaurefactoring.exception.BureauCallMaskedApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/bureau")
public class BureauController {
    Logger logger = LoggerFactory.getLogger(BureauController.class);

    @Autowired
    BureauService bureauService;

    @RequestMapping(value = "/fetchBureau", method = RequestMethod.POST, consumes = "application/json", produces = "application/json")
    public ResponseEntity<ResponseDTO<BureauResponseDTO>> fetchBureau(@RequestBody ExperianDetailsDTO requestDTO, @RequestParam(required = false, defaultValue = BureauConstants.MINIMUM_BUREAU_TIME_GAP) Long days, @RequestParam(required = false, defaultValue = "false") boolean requireBureauResponse) throws Exception {

        logger.info("Request to fetch bureau v2: {}", requestDTO);


        if (ObjectUtils.isEmpty(requestDTO.getFirstName()) || ObjectUtils.isEmpty(requestDTO.getLastName()) || ObjectUtils.isEmpty(requestDTO.getSource())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ResponseDTO<>(false, "Required fields does not exist"));
        }

        try {
            return new ResponseEntity<>(bureauService.BureauDetails(requestDTO, days, requireBureauResponse), HttpStatus.OK);
        } catch(BureauCallMaskedApiException e){
            throw(e);
        } catch (Exception e) {
            logger.error("Exception while updating bureau details---", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }
    }

}
