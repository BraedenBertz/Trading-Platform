package utilities;

public enum Operator{
	lessThan {
		public boolean compare(double conditional, double discriminator) { return conditional > discriminator; }
	},
	lessThanOrEqual {
		public boolean compare(double conditional, double discriminator) { return conditional >= discriminator; }
	},
	greaterThan {
		public boolean compare(double conditional, double discriminator) { return conditional <= discriminator; }
	},
	greaterThanOrEqual {
		public boolean compare(double conditional, double discriminator) { return conditional <= discriminator; }
	},
	equals {
		public boolean compare(double conditional, double discriminator) { return conditional == discriminator; }
	},
	doesNotEqual {
		public boolean compare(double conditional, double discriminator) { return conditional != discriminator; }
	};
	
	public abstract boolean compare(double conditional, double discriminator);
	
}