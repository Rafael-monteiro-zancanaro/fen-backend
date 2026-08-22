package org.fen.fen.infra;

public interface Converters {


     interface SimpleConverter<E extends WithSynteticId, D> {
        default D toDTO(E input) {
            throw new UnsupportedOperationException("toDTO não implementado.");
        }

        default E fromDTO(D input) {
            throw new UnsupportedOperationException("fromDTO não implementado.");
        }
    }

    interface BidirectionalConverter<E extends WithSynteticId, RQ, RP> {
        default RQ toRequestDTO(E input) {
            throw new UnsupportedOperationException("toDTO não implementado.");
        }

        default RP toResponseDTO(E input) {
            throw new UnsupportedOperationException("toDTO não implementado.");
        }

        default E fromRequestDTO(RQ input) {
            throw new UnsupportedOperationException("fromDTO não implementado.");
        }

        default E fromResponseDTO(RP input) {
            throw new UnsupportedOperationException("fromDTO não implementado.");
        }

    }

}
