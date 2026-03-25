import { gql } from "@apollo/client";

export const JOIN_GAME = gql`
mutation JoinGame($gameMode: GameMode!) {
    joinGame(gameMode: $gameMode) {
        message
        game {
            board
            createdAt
            gameMode
            id
            movecounter
            updatedAt
            winner
            player2 {
                email
                id
                nickname
            }
            player1 {
                email
                id
                nickname
            }
        }
    }
}
`
