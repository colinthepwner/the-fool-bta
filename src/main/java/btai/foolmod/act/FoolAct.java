package btai.foolmod.act;

import btai.foolmod.entity.FoolEntity;

public interface FoolAct {

	boolean seek(FoolEntity fool);

	boolean tick(FoolEntity fool);

	String name();
}
