package com.idlecard.game.card.cardController;

import com.idlecard.game.card.cardDTO.response.CardDefinitionDto;
import com.idlecard.game.card.cardService.CardCatalogService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/cards")
@RequiredArgsConstructor
public class CardController {

    private final CardCatalogService cardCatalogService;

    @GetMapping
    public List<CardDefinitionDto> listAll() {
        return cardCatalogService.all().stream().map(CardDefinitionDto::from).toList();
    }
}
