import {LEFT, RIGHT} from "@/components/WorkItemHeader";

export const MOVED_UP = 0;
const MOVED_DOWN = 1;

/**
 * The marker of a moved work item, on the side where it is not at this position. The target is the new state:
 * at the source position, the target side reads "moved to"; at the target position, the source side reads "moved from".
 */
export default function MovedItem({side, movedOutlineNumber, moveDirection}) {
  return (
      <span>
        {side === LEFT && moveDirection === MOVED_UP
            && <>
              <a href={`#${side}${movedOutlineNumber}`}>moved from {movedOutlineNumber}</a>
              <svg viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                <g id="SVGRepo_bgCarrier" strokeWidth="0"></g>
                <g id="SVGRepo_tracerCarrier" strokeLinecap="round" strokeLinejoin="round"></g>
                <g id="SVGRepo_iconCarrier">
                  <path d="M8 7L8 9C8 11.2091 9.79086 13 12 13L17 13" stroke="#666666" strokeWidth="2"
                        strokeLinecap="round" strokeLinejoin="round"></path>
                  <path d="M14 10L17 13L14 16" stroke="#666666" strokeWidth="2" strokeLinecap="round"
                        strokeLinejoin="round"></path>
                </g>
              </svg>
            </>}
        {side === LEFT && moveDirection === MOVED_DOWN
            && <>
              <a href={`#${side}${movedOutlineNumber}`}>moved from {movedOutlineNumber}</a>
              <svg viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                <g id="SVGRepo_bgCarrier" strokeWidth="0"></g>
                <g id="SVGRepo_tracerCarrier" strokeLinecap="round" strokeLinejoin="round"></g>
                <g id="SVGRepo_iconCarrier">
                  <path d="M8 17L8 15C8 12.7909 9.79086 11 12 11L17 11" stroke="#666666" strokeWidth="2"
                        strokeLinecap="round" strokeLinejoin="round"></path>
                  <path d="M14 8L17 11L14 14" stroke="#666666" strokeWidth="2" strokeLinecap="round"
                        strokeLinejoin="round"></path>
                </g>
              </svg>
            </>}
        {side === RIGHT && moveDirection === MOVED_UP
            && <>
              <svg viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                <g id="SVGRepo_bgCarrier" strokeWidth="0"></g>
                <g id="SVGRepo_tracerCarrier" strokeLinecap="round" strokeLinejoin="round"></g>
                <g id="SVGRepo_iconCarrier">
                  <path d="M7 16L9 16C11.2091 16 13 14.2091 13 12L13 7" stroke="#666666" strokeWidth="2"
                        strokeLinecap="round" strokeLinejoin="round"></path>
                  <path d="M16 10L13 7L10 10" stroke="#666666" strokeWidth="2" strokeLinecap="round"
                        strokeLinejoin="round"></path>
                </g>
              </svg>
              <a href={`#${side}${movedOutlineNumber}`}>moved to {movedOutlineNumber}</a>
            </>}
        {side === RIGHT && moveDirection === MOVED_DOWN
            && <>
              <svg viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                <g id="SVGRepo_bgCarrier" strokeWidth="0"></g>
                <g id="SVGRepo_tracerCarrier" strokeLinecap="round" strokeLinejoin="round"></g>
                <g id="SVGRepo_iconCarrier">
                  <path d="M7 8L9 8C11.2091 8 13 9.79086 13 12L13 17" stroke="#666666" strokeWidth="2"
                        strokeLinecap="round" strokeLinejoin="round"></path>
                  <path d="M16 14L13 17L10 14" stroke="#666666" strokeWidth="2" strokeLinecap="round"
                        strokeLinejoin="round"></path>
                </g>
              </svg>
              <a href={`#${side}${movedOutlineNumber}`}>moved to {movedOutlineNumber}</a>
            </>}

      </span>
  );
}
