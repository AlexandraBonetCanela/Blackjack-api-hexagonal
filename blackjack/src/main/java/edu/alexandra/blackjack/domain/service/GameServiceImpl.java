package edu.alexandra.blackjack.domain.service;

import edu.alexandra.blackjack.application.rest.request.CreateGameRequest;
import edu.alexandra.blackjack.application.rest.request.PlayGameRequest;
import edu.alexandra.blackjack.application.rest.response.GameResponse;
import edu.alexandra.blackjack.domain.*;
import edu.alexandra.blackjack.domain.exception.GameNotFoundException;
import edu.alexandra.blackjack.domain.repository.GameRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Service
public class GameServiceImpl implements GameService{

    private final GameRepository gameRepository;
    private final PlayerService playerService;

    @Override
    public Mono<GameResponse> createGame(CreateGameRequest newGame) {

        Game game = Game.builder()
                .id(UUID.randomUUID().toString())
                .moneyBet(newGame.getMoneyBet())
                .status(GameStatus.STARTED)
                .build()
                .dealInitialCards();

        return playerService.getOrCreatePlayer(newGame.getPlayerName())
                .flatMap(player -> {
                    game.setPlayer(player);
                    return gameRepository.save(game);
                })
                .map(this::toGameResponse);
    }


    @Override
    public Mono<GameResponse> getGame(String id) {
        return gameRepository.findById(id)
                .map(this::toGameResponse);
    }

    @Override
    public Mono<GameResponse> playGame(String id, PlayGameRequest move) {

        return gameRepository.findById(id)
                .flatMap(game -> game.executeGameLogic(move.getMoveType()))
                .flatMap( game -> {
                    if ( game.getStatus() == GameStatus.FINISHED) {
                        return finishGame(game);
                    }
                    return gameRepository.save(game);
                })
                .map(this::toGameResponse)
                .switchIfEmpty(Mono.error(new GameNotFoundException(id)));
    }

    @Override
    public Mono<Boolean> deleteGame(String id) {
        return gameRepository.deleteById(id);
    }

    private Mono<Game> finishGame(Game game) {
        if (game.getGameResult() == GameResult.WON) {
            Player player = game.getPlayer();
            player.setTotalScore(player.getTotalScore().add(game.getMoneyBet().multiply(BigDecimal.valueOf(2))));

            return playerService.updatePlayerScore(player)
                    .then(gameRepository.save(game));
        }

        return gameRepository.save(game);
    }

    private GameResponse toGameResponse(Game game) {

        return new GameResponse(
                game.getId(),
                game.getPlayer(),
                game.getPlayerHand(),
                maskDealerCard(game.getDealerHand(), game.getStatus()),
                game.getMoneyBet(),
                game.getStatus(),
                game.getGameResult());
    }

    private List<Card> maskDealerCard(List<Card> dealerHand, GameStatus gameStatus){

        if (gameStatus == GameStatus.FINISHED || dealerHand == null || dealerHand.isEmpty()) {
            return dealerHand;
        }

        List<Card> maskedHand = new ArrayList<>(dealerHand);
        int lastCardIndex = maskedHand.size() -1;

        maskedHand.set(lastCardIndex, new Card("SECRET", "SECRET"));

        return maskedHand;
    }
}
