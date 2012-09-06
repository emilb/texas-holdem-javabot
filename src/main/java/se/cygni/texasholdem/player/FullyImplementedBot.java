package se.cygni.texasholdem.player;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.cygni.texasholdem.client.CurrentPlayState;
import se.cygni.texasholdem.client.PlayerClient;
import se.cygni.texasholdem.communication.message.event.*;
import se.cygni.texasholdem.communication.message.request.ActionRequest;
import se.cygni.texasholdem.game.Action;
import se.cygni.texasholdem.game.Card;
import se.cygni.texasholdem.game.GamePlayer;
import se.cygni.texasholdem.game.Room;
import se.cygni.texasholdem.game.definitions.Rank;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class FullyImplementedBot implements Player {

    private static Logger log = LoggerFactory
            .getLogger(FullyImplementedBot.class);

    private final String serverHost;
    private final int serverPort;
    private final PlayerClient playerClient;

    public FullyImplementedBot(String serverHost, int serverPort) {
        this.serverHost = serverHost;
        this.serverPort = serverPort;

        // Initialize the player client
        playerClient = new PlayerClient(this, serverHost, serverPort);
    }

    public void playATrainingGame() throws Exception {
        playerClient.connect();
        playerClient.registerForPlay(Room.TRAINING);
    }

    @Override
    public String getName() {
        throw new RuntimeException("Did you forget to specify a name for your bot?");
    }

    @Override
    public void serverIsShuttingDown(ServerIsShuttingDownEvent event) {
    }

    @Override
    public void onPlayIsStarted(PlayIsStartedEvent event) {
    }

    @Override
    public void onTableChangedStateEvent(TableChangedStateEvent event) {
    }

    @Override
    public void onYouHaveBeenDealtACard(YouHaveBeenDealtACardEvent event) {
    }

    @Override
    public void onCommunityHasBeenDealtACard(CommunityHasBeenDealtACardEvent event) {
    }

    @Override
    public void onPlayerBetBigBlind(PlayerBetBigBlindEvent event) {
    }

    @Override
    public void onPlayerBetSmallBlind(PlayerBetSmallBlindEvent event) {
    }

    @Override
    public void onPlayerFolded(PlayerFoldedEvent event) {
    }

    @Override
    public void onPlayerCalled(PlayerCalledEvent event) {
    }

    @Override
    public void onPlayerRaised(PlayerRaisedEvent event) {
    }

    @Override
    public void onPlayerWentAllIn(PlayerWentAllInEvent event) {
    }

    @Override
    public void onPlayerChecked(PlayerCheckedEvent event) {
    }

    @Override
    public void onYouWonAmount(YouWonAmountEvent event) {
    }

    @Override
    public void onShowDown(ShowDownEvent event) {
    }

    @Override
    public void onTableIsDone(TableIsDoneEvent event) {
        log.debug("Table is done, I'm leaving the table with ${}", playerClient.getCurrentPlayState().getMyCurrentChipAmount());
    }

    @Override
    public void onPlayerQuit(PlayerQuitEvent event) {
    }

    @Override
    public Action actionRequired(ActionRequest request) {

        Action callAction = null;
        Action checkAction = null;
        Action raiseAction = null;
        Action foldAction = null;

        for (final Action action : request.getPossibleActions()) {
            switch (action.getActionType()) {
                case CALL:
                    callAction = action;
                    break;
                case CHECK:
                    checkAction = action;
                    break;
                case FOLD:
                    foldAction = action;
                    break;
                case RAISE:
                    raiseAction = action;
                    break;
                default:
                    break;
            }
        }

        // This player becomes suspicious if another player has bet
        // more than 4 x BigBlind.
        boolean someOneHasBetAlot = false;

        // The current play state is accessible through this class. It
        // keeps track of basic events and players.
        CurrentPlayState playState = playerClient.getCurrentPlayState();
        long currentBB = playState.getBigBlind();

        for (GamePlayer player : playState.getPlayers()) {
            if (playState.getInvestmentInPotFor(player) >= currentBB * 4) {
                someOneHasBetAlot = true;
                break;
            }
        }

        // If we have a pair, be more aggressive
        List<Card> allMyCards = new ArrayList<Card>(playState.getMyCards());
        allMyCards.addAll(playState.getCommunityCards());
        boolean iHaveApair = containsPair(allMyCards);


        Action action = null;
        if (someOneHasBetAlot && checkAction != null)
            action = checkAction;
        else if (someOneHasBetAlot && !iHaveApair)
            action = foldAction;
        else if (iHaveApair && callAction != null)
            action = callAction;
        else if (iHaveApair && raiseAction != null)
            action = raiseAction;
        else if (!iHaveApair && !someOneHasBetAlot) {
            if (checkAction != null)
                action = checkAction;
            else if (callAction != null)
                action = callAction;
            else
                action = foldAction;
        }
        // failsafe
        if (action == null)
            action = foldAction;

        log.debug("{} returning action: {}", getName(), action);
        return action;
    }

    @Override
    public void connectionToGameServerLost() {
        log.info("Connection to game server is lost. Exit time");
        System.exit(0);
    }

    @Override
    public void connectionToGameServerEstablished() {
    }

    /**
     *
     * @param cards
     * @return TRUE if there are at least two cards of the same rank
     */
    private boolean containsPair(List<Card> cards) {

        // First sort the cards
        Collections.sort(cards, new Comparator<Card>() {
            @Override
            public int compare(Card first, Card second) {
                final Integer firstVal = Integer.valueOf(first.getRank()
                        .getOrderValue());
                final Integer secondVal = Integer.valueOf(second.getRank()
                        .getOrderValue());

                final int comparison = firstVal.compareTo(secondVal);
                if (comparison == 0) {
                    final String firstStrVal = first.getSuit().getShortName();
                    final String secondStrVal = second.getSuit().getShortName();

                    return firstStrVal.compareTo(secondStrVal);
                }

                return comparison;
            }
        });

        // Loop over the cards, if two cards after one another have the same
        // rank return true
        Rank lastRank = null;
        for (Card card : cards) {
            if (lastRank == null)
                continue;

            if (card.getRank() == lastRank)
                return true;
        }

        return false;
    }

    public static void main(String... args) {
        FullyImplementedBot bot = new FullyImplementedBot("192.168.10.100", 4711);

        try {
            bot.playATrainingGame();

        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }
}
