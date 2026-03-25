
import { gql } from '@apollo/client';

export const FETCH_GAMESTATE = gql`
  query FetchGamestate {
      fetchGamestate {
          board
          createdAt
          gameMode
          id
          movecounter
          updatedAt
          winner
          currentturn {
              email
              id
              nickname
          }
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
`
